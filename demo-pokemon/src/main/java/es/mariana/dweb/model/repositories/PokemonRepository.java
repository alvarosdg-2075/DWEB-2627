package es.mariana.dweb.model.repositories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import es.mariana.dweb.model.entities.Pokemon;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

@Repository
public class PokemonRepository {

    private static final String POKEMON_API_URL = "https://pokeapi.co/api/v2";
    private static final int MAX_POKEMON_ID = 1025;

    private final RestTemplate restTemplate;
    private final Random random;

    public PokemonRepository() {
        this(new RestTemplate());
    }

    public PokemonRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.random = new Random();
    }

    public Pokemon findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        return restTemplate.getForObject(POKEMON_API_URL + "/pokemon/" + normalizedName, Pokemon.class);
    }

    public Pokemon findRandom() {
        int randomId = random.nextInt(MAX_POKEMON_ID) + 1;
        return findById(randomId);
    }

    public Pokemon findById(int id) {
        if (id < 1 || id > MAX_POKEMON_ID) {
            throw new IllegalArgumentException("El id del pokémon debe estar entre 1 y " + MAX_POKEMON_ID);
        }

        return restTemplate.getForObject(POKEMON_API_URL + "/pokemon/" + id, Pokemon.class);
    }

    public ArrayList<Pokemon> findRandomList(int limit) {
        ArrayList<Pokemon> result = new ArrayList<>();
        if (limit <= 0) {
            return result;
        }

        Set<Integer> generatedIds = new HashSet<>();
        while (generatedIds.size() < Math.min(limit, MAX_POKEMON_ID)) {
            generatedIds.add(random.nextInt(MAX_POKEMON_ID) + 1);
        }

        for (Integer id : generatedIds) {
            Pokemon pokemon = findById(id);
            if (pokemon != null) {
                result.add(pokemon);
            }
            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    public ArrayList<Pokemon> findAll() {
        return findRandomList(10);
    }

    /**
     * Devuelve todos los tipos que ofrece PokéAPI (name + url).
     */
    public ArrayList<Pokemon.Type> findAllTypes() {
        TypeListResponse response = restTemplate.getForObject(
                POKEMON_API_URL + "/type?limit=100", TypeListResponse.class);

        if (response == null || response.getResults() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(response.getResults());
    }

    /**
     * Devuelve como máximo {@code limit} Pokémon del tipo indicado.
     * Se descartan las formas especiales cuyo id supera MAX_POKEMON_ID,
     * porque findById no las admite.
     */
    public ArrayList<Pokemon> findByType(String typeName, int limit) {
        ArrayList<Pokemon> result = new ArrayList<>();
        if (typeName == null || typeName.isBlank() || limit <= 0) {
            return result;
        }

        String normalizedType = typeName.trim().toLowerCase(Locale.ROOT);
        TypeDetailResponse response;
        try {
            response = restTemplate.getForObject(
                    POKEMON_API_URL + "/type/{name}", TypeDetailResponse.class, normalizedType);
        } catch (HttpClientErrorException.NotFound e) {
            return result;
        }

        if (response == null || response.getPokemon() == null) {
            return result;
        }

        for (TypePokemonEntry entry : response.getPokemon()) {
            if (result.size() >= limit) {
                break;
            }
            if (entry.getPokemon() == null) {
                continue;
            }

            int id = extractId(entry.getPokemon().getUrl());
            if (id < 1 || id > MAX_POKEMON_ID) {
                continue;
            }

            Pokemon pokemon = findById(id);
            if (pokemon != null) {
                result.add(pokemon);
            }
        }

        return result;
    }

    /* Extrae el id del final de una URL como https://pokeapi.co/api/v2/pokemon/6/ */
    private int extractId(String url) {
        if (url == null || url.isBlank()) {
            return -1;
        }
        String[] parts = url.split("/");
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /* Clases auxiliares para deserializar las respuestas de /type y /type/{name}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypeListResponse {
        private ArrayList<Pokemon.Type> results = new ArrayList<>();

        public ArrayList<Pokemon.Type> getResults() {
            return results;
        }

        public void setResults(ArrayList<Pokemon.Type> results) {
            this.results = results;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypeDetailResponse {
        private ArrayList<TypePokemonEntry> pokemon = new ArrayList<>();

        public ArrayList<TypePokemonEntry> getPokemon() {
            return pokemon;
        }

        public void setPokemon(ArrayList<TypePokemonEntry> pokemon) {
            this.pokemon = pokemon;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TypePokemonEntry {
        private int slot;
        private Pokemon.Type pokemon;

        public int getSlot() {
            return slot;
        }

        public void setSlot(int slot) {
            this.slot = slot;
        }

        public Pokemon.Type getPokemon() {
            return pokemon;
        }

        public void setPokemon(Pokemon.Type pokemon) {
            this.pokemon = pokemon;
        }
    }
}
