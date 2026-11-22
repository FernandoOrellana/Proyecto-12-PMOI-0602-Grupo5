using FluentValidation;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PetCare.Api.Infraestructura;
using PetCare.Application.Abstracciones;
using PetCare.Application.Contratos;
using PetCare.Application.Servicios;
using PetCare.Domain;

namespace PetCare.Api.Controllers;

[ApiController]
[Route("api/citas")]
[Authorize]
public class CitasController(
    CitasService citas,
    IValidator<CitaRequest> vCita,
    IValidator<ConsultaRequest> vConsulta,
    IValidator<ObservacionesRequest> vObservaciones) : ControladorBase
{
    [HttpPost]
    [Authorize(Roles = Roles.Dueno)]
    public async Task<IActionResult> Agendar([FromBody] CitaRequest r, CancellationToken ct)
    {
        await ValidarAsync(vCita, r);
        return Respuesta(await citas.AgendarAsync(Actor, r, ct), StatusCodes.Status201Created);
    }

    [HttpGet]
    public async Task<IActionResult> Listar([FromQuery] DateOnly? fecha, [FromQuery] EstadoCita? estado,
        [FromQuery] int page = 1, [FromQuery] int limit = 20, CancellationToken ct = default)
    {
        var (p, l) = ValidarPaginacion(page, limit);
        var filtro = new FiltroCitas(null, null, null, fecha, estado, p, l);
        return Lista(await citas.ListarAsync(Actor, filtro, ct));
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> Obtener(int id, CancellationToken ct) =>
        Respuesta(await citas.ObtenerAsync(Actor, id, ct));

    [HttpPatch("{id:int}/cancelar")]
    [Authorize(Roles = Roles.Dueno)]
    public async Task<IActionResult> Cancelar(int id, CancellationToken ct) =>
        Respuesta(await citas.CancelarAsync(Actor, id, ct));

    [HttpPatch("{id:int}/iniciar")]
    [Authorize(Roles = Roles.Veterinario)]
    public async Task<IActionResult> IniciarConsulta(int id, CancellationToken ct) =>
        Respuesta(await citas.IniciarConsultaAsync(Actor, id, ct));

    [HttpPost("{id:int}/consulta")]
    [Authorize(Roles = Roles.Veterinario)]
    public async Task<IActionResult> RegistrarConsulta(int id, [FromBody] ConsultaRequest r, CancellationToken ct)
    {
        await ValidarAsync(vConsulta, r);
        return Respuesta(await citas.RegistrarConsultaAsync(Actor, id, r, ct), StatusCodes.Status201Created);
    }

    [HttpPatch("{id:int}/observaciones")]
    [Authorize(Roles = $"{Roles.Administrador},{Roles.Veterinario}")]
    public async Task<IActionResult> ActualizarObservaciones(int id, [FromBody] ObservacionesRequest r, CancellationToken ct)
    {
        await ValidarAsync(vObservaciones, r);
        return Respuesta(await citas.ActualizarObservacionesAsync(Actor, id, r.Observaciones, ct));
    }
}
