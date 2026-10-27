package es.mariana.dweb.controller;

import es.mariana.dweb.model.entities.Pokemon;
import es.mariana.dweb.model.entities.PokemonHeightComparator;
import es.mariana.dweb.model.entities.PokemonNameComparator;
import es.mariana.dweb.model.entities.PokemonWeightComparator;
import es.mariana.dweb.model.service.PokemonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

@Controller
public class PokemonController {

    private static final String SESION_ENTRENADOR = "entrenador";
    private static final String SESION_EQUIPO = "equipo";
    private static final int MAX_EQUIPO = 6;

    @Autowired
    private PokemonService pokemonService;

    @GetMapping("/")
    public String home(Model model) {
        ArrayList<Pokemon> pokemons = pokemonService.obtenerPokemons();
        model.addAttribute("pokemons", pokemons);

        return "home";
    }

    @GetMapping("/informacion")
    public String informacion(Model model) {
        Pokemon pokemon = pokemonService.findRandom();

        model.addAttribute("titulo", "Pokémon destacado");
        model.addAttribute("presentacion", "Conoce los datos principales de uno de los Pokémon disponibles en nuestra Pokédex.");
        model.addAttribute("fechaActual", LocalDateTime.now());
        model.addAttribute("pokemon", pokemon);
        model.addAttribute("destacado", true);

        return "informacion";
    }

    @GetMapping("/pokemon/tabla")
    public String listadoTabla(
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "alturaMin", required = false) Double alturaMin,
            @RequestParam(name = "pesoMax", required = false) Double pesoMax,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "direccion", required = false) String direccion,
            Model model) {

        ArrayList<Pokemon> pokemons;
        if (tipo == null || tipo.isBlank()) {
            pokemons = pokemonService.findRandomList(10);
        } else {
            pokemons = pokemonService.findByType(tipo);
        }

        pokemons = filtrarYOrdenar(pokemons, nombre, alturaMin, pesoMax, orden, direccion);

        model.addAttribute("titulo", "Pokémon en formato tabla");
        model.addAttribute("fechaActual", LocalDateTime.now());
        model.addAttribute("pokemons", pokemons);
        model.addAttribute("tipos", pokemonService.findAllTypes());
        model.addAttribute("tipo", tipo);
        model.addAttribute("nombre", nombre);
        model.addAttribute("alturaMin", alturaMin);
        model.addAttribute("pesoMax", pesoMax);
        model.addAttribute("orden", orden);
        model.addAttribute("direccion", direccion);

        return "listado-tabla";
    }

    @GetMapping("/pokemon/galeria")
    public String galeria(
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "alturaMin", required = false) Double alturaMin,
            @RequestParam(name = "pesoMax", required = false) Double pesoMax,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "direccion", required = false) String direccion,
            Model model) {

        ArrayList<Pokemon> pokemons;
        if (tipo == null || tipo.isBlank()) {
            pokemons = pokemonService.findRandomList(12);
        } else {
            pokemons = pokemonService.findByType(tipo);
        }

        pokemons = filtrarYOrdenar(pokemons, nombre, alturaMin, pesoMax, orden, direccion);

        model.addAttribute("titulo", "Galería Pokémon");
        model.addAttribute("pokemons", pokemons);
        model.addAttribute("tipos", pokemonService.findAllTypes());
        model.addAttribute("tipo", tipo);
        model.addAttribute("nombre", nombre);
        model.addAttribute("alturaMin", alturaMin);
        model.addAttribute("pesoMax", pesoMax);
        model.addAttribute("orden", orden);
        model.addAttribute("direccion", direccion);

        return "galeria-pokemon";
    }

    @GetMapping("/pokemon/{id}")
    public String detalle(@PathVariable("id") int id, Model model) {
        Pokemon pokemon;
        try {
            pokemon = pokemonService.findById(id);
        } catch (IllegalArgumentException | RestClientException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pokémon no encontrado", e);
        }

        if (pokemon == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pokémon no encontrado");
        }

        model.addAttribute("titulo", "Ficha de " + capitalizar(pokemon.getName()));
        model.addAttribute("presentacion", "Consulta los datos principales del Pokémon seleccionado.");
        model.addAttribute("fechaActual", LocalDateTime.now());
        model.addAttribute("pokemon", pokemon);
        model.addAttribute("destacado", false);

        return "informacion";
    }

    /* ---------------------------------------------------------
       BATALLA
       - /pokemon/batalla        -> dos Pokémon aleatorios (comportamiento original)
       - /pokemon/batalla/{id}   -> un Pokémon del equipo contra un rival aleatorio
       --------------------------------------------------------- */
    @GetMapping({"/pokemon/batalla", "/pokemon/batalla/{id}"})
    public String batalla(@PathVariable(name = "id", required = false) Integer id,
                          HttpSession session, Model model) {
        Pokemon pokemon1;
        if (id == null) {
            pokemon1 = pokemonService.findRandom();
        } else {
            // Solo pueden combatir los Pokémon que pertenecen al equipo de la sesión
            pokemon1 = obtenerEquipo(session).stream()
                    .filter(p -> p.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (pokemon1 == null) {
                return "redirect:/pokemon/equipo";
            }
        }
        Pokemon pokemon2 = pokemonService.findRandom();

        model.addAttribute("pokemon1", pokemon1);
        model.addAttribute("pokemon2", pokemon2);
        model.addAttribute("desdeEquipo", id != null);

        return "batalla-pokemon";
    }

    /* ---------------------------------------------------------
       ENTRENADOR Y EQUIPO (todo en HttpSession)
       --------------------------------------------------------- */
    @GetMapping("/pokemon/equipo")
    public String equipo(HttpSession session, Model model) {
        Object entrenador = session.getAttribute(SESION_ENTRENADOR);

        model.addAttribute("titulo", "Mi equipo Pokémon");
        model.addAttribute("entrenador", entrenador);
        model.addAttribute("equipo", entrenador == null ? new ArrayList<Pokemon>() : obtenerEquipo(session));
        model.addAttribute("maxEquipo", MAX_EQUIPO);

        return "equipo";
    }

    @PostMapping("/pokemon/entrenador")
    public String registrarEntrenador(@RequestParam(name = "nombre", required = false) String nombre,
                                      HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute(SESION_ENTRENADOR) != null) {
            // Ya hay un entrenador: no se sobrescribe ni se vacía su equipo
            return "redirect:/pokemon/equipo";
        }

        if (nombre == null || nombre.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del entrenador no puede estar vacío.");
            return "redirect:/pokemon/equipo";
        }

        session.setAttribute(SESION_ENTRENADOR, nombre.trim());
        session.setAttribute(SESION_EQUIPO, new ArrayList<Pokemon>());

        return "redirect:/pokemon/equipo";
    }

    @PostMapping("/pokemon/equipo/anadir")
    public String anadirAlEquipo(
            @RequestParam("id") int id,
            @RequestParam(name = "origen", required = false) String origen,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "alturaMin", required = false) Double alturaMin,
            @RequestParam(name = "pesoMax", required = false) Double pesoMax,
            @RequestParam(name = "orden", required = false) String orden,
            @RequestParam(name = "direccion", required = false) String direccion,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (session.getAttribute(SESION_ENTRENADOR) == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Debes registrar un entrenador antes de añadir Pokémon al equipo.");
            return "redirect:/pokemon/equipo";
        }

        ArrayList<Pokemon> equipo = obtenerEquipo(session);
        String error = null;

        Pokemon repetido = equipo.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        if (repetido != null) {
            error = capitalizar(repetido.getName()) + " ya forma parte de tu equipo.";
        } else if (equipo.size() >= MAX_EQUIPO) {
            error = "Tu equipo está completo: no puede tener más de " + MAX_EQUIPO + " Pokémon.";
        } else {
            try {
                Pokemon pokemon = pokemonService.findById(id);
                if (pokemon != null) {
                    equipo.add(pokemon);
                }
            } catch (IllegalArgumentException | RestClientException e) {
                error = "No se ha podido añadir el Pokémon seleccionado.";
            }
        }

        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
        }

        // Se vuelve al mismo listado conservando filtros y ordenación en la URL
        addIfPresent(redirectAttributes, "tipo", tipo);
        addIfPresent(redirectAttributes, "nombre", nombre);
        addIfPresent(redirectAttributes, "alturaMin", alturaMin);
        addIfPresent(redirectAttributes, "pesoMax", pesoMax);
        addIfPresent(redirectAttributes, "orden", orden);
        addIfPresent(redirectAttributes, "direccion", direccion);

        return "redirect:" + ("galeria".equals(origen) ? "/pokemon/galeria" : "/pokemon/tabla");
    }

    @PostMapping("/pokemon/equipo/quitar")
    public String quitarDelEquipo(@RequestParam("id") int id, HttpSession session) {
        if (session.getAttribute(SESION_ENTRENADOR) != null) {
            obtenerEquipo(session).removeIf(p -> p.getId() == id);
        }
        return "redirect:/pokemon/equipo";
    }

    @PostMapping("/pokemon/cerrar-sesion")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/pokemon/equipo";
    }

    /**
     * Método auxiliar compartido por la tabla y la galería.
     * - nombre: coincidencia parcial sin distinguir mayúsculas (vacío = no filtra).
     * - alturaMin: metros; la API guarda decímetros, de ahí el / 10.0.
     * - pesoMax: kilogramos; la API guarda hectogramos, de ahí el / 10.0.
     * - orden: "nombre", "altura" o "peso" (vacío = se conserva el orden de recuperación).
     * - direccion: "desc" invierte el orden; cualquier otro valor es ascendente.
     */
    private ArrayList<Pokemon> filtrarYOrdenar(ArrayList<Pokemon> pokemons, String nombre,
                                               Double alturaMin, Double pesoMax,
                                               String orden, String direccion) {
        ArrayList<Pokemon> resultado = new ArrayList<>(pokemons);

        if (nombre != null && !nombre.isBlank()) {
            String buscado = nombre.trim().toLowerCase(Locale.ROOT);
            resultado.removeIf(p -> p.getName() == null
                    || !p.getName().toLowerCase(Locale.ROOT).contains(buscado));
        }

        if (alturaMin != null) {
            resultado.removeIf(p -> p.getHeight() / 10.0 < alturaMin);
        }

        if (pesoMax != null) {
            resultado.removeIf(p -> p.getWeight() / 10.0 > pesoMax);
        }

        if (orden != null && !orden.isBlank()) {
            Comparator<Pokemon> comparador = switch (orden) {
                case "nombre" -> new PokemonNameComparator();
                case "altura" -> new PokemonHeightComparator();
                case "peso" -> new PokemonWeightComparator();
                default -> null;
            };

            if (comparador != null) {
                if ("desc".equals(direccion)) {
                    comparador = comparador.reversed();
                }
                resultado.sort(comparador);
            }
        }

        return resultado;
    }

    /* Devuelve el equipo guardado en la sesión; lo crea vacío si todavía no existe. */
    @SuppressWarnings("unchecked")
    private ArrayList<Pokemon> obtenerEquipo(HttpSession session) {
        ArrayList<Pokemon> equipo = (ArrayList<Pokemon>) session.getAttribute(SESION_EQUIPO);
        if (equipo == null) {
            equipo = new ArrayList<>();
            session.setAttribute(SESION_EQUIPO, equipo);
        }
        return equipo;
    }

    private void addIfPresent(RedirectAttributes redirectAttributes, String nombre, Object valor) {
        if (valor != null && !valor.toString().isBlank()) {
            redirectAttributes.addAttribute(nombre, valor);
        }
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

}
