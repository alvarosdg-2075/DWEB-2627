package es.mariana.dweb.controller;

import es.mariana.dweb.model.service.EntrenadorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EntrenadorController {
    @Autowired
    private EntrenadorService servicio;

    // RESUELTO: listado de entrenadores.
    @GetMapping("/entrenadores")
    public String listado(Model model, HttpSession session) {
        model.addAttribute("entrenadores", servicio.findAll());
        model.addAttribute("entrenadorSeleccionado", session.getAttribute("entrenador"));
        return "entrenadores";
    }

    // RESUELTO: selección; se guarda el OBJETO COMPLETO en sesión.
    @PostMapping("/entrenadores/seleccionar")
    public String seleccionar(@RequestParam int id, HttpSession session) {
        session.setAttribute("entrenador", servicio.findById(id));
        return "redirect:/entrenadores";
    }

    // RESUELTO: deselección sin eliminar al entrenador ni su equipo.
    @PostMapping("/entrenadores/salir")
    public String salir(HttpSession session) {
        session.removeAttribute("entrenador");
        return "redirect:/entrenadores";
    }

    // TODO ALUMNADO: GET /entrenadores/nuevo -> mostrar formulario de creación.
    // TODO ALUMNADO: POST /entrenadores/crear -> llamar al servicio, capturar EntrenadorException.
    // TODO ALUMNADO: GET /entrenadores/{id}/editar -> precargar el formulario.
    // TODO ALUMNADO: POST /entrenadores/{id}/editar -> guardar, mostrar errores y refrescar sesión si corresponde.
    // TODO ALUMNADO: POST /entrenadores/{id}/eliminar -> borrar y quitar de sesión si estaba seleccionado.
}
