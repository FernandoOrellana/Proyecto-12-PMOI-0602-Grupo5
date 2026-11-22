using PetCare.Application.Abstracciones;
using PetCare.Application.Common;
using PetCare.Application.Contratos;
using PetCare.Domain;

namespace PetCare.Application.Servicios;

public class CitasService(
    ICitaRepositorio citas,
    IMascotaRepositorio mascotas,
    ICatalogoRepositorio catalogo,
    IVeterinarioRepositorio veterinarios,
    IUnitOfWork uow,
    IReloj reloj,
    Mapeador mapeador)
{
    public async Task<CitaDto> AgendarAsync(Actor actor, CitaRequest r, CancellationToken ct)
    {
        var mascota = await mascotas.PorIdAsync(r.MascotaId, ct);
        if (mascota is null || !mascota.Activo) throw new NoEncontradoException("Mascota no encontrada");
        if (mascota.DuenoId != actor.Id) throw new ProhibidoException("Solo puedes agendar citas para tus mascotas");

        var servicio = await catalogo.ServicioPorIdAsync(r.ServicioId, ct);
        if (servicio is null || !servicio.Activo) throw new ValidacionException("El servicio seleccionado no está disponible");

        var veterinario = await veterinarios.PorIdAsync(r.VeterinarioId, ct)
            ?? throw new ValidacionException("El veterinario seleccionado no existe");

        if (!TimeOnly.TryParseExact(r.Hora, "HH:mm", out var hora))
            throw new ValidacionException("Use el formato HH:mm para la hora");
        if (!Reglas.EsFechaHoraFutura(r.Fecha, hora, reloj.Ahora))
            throw new ValidacionException("La fecha y hora deben ser futuras");

        var disponibles = Reglas.HorasDisponibles(
            r.Fecha,
            veterinario.Horarios.Select(h => new Franja(h.DiaSemana, h.HoraInicio, h.HoraFin)),
            await citas.ActivasPorVeterinarioYFechaAsync(r.VeterinarioId, r.Fecha, ct),
            servicio.DuracionMin,
            reloj.Ahora);
        if (!disponibles.Contains(hora)) throw new ConflictoException("La hora seleccionada no está disponible");

        var cita = new Cita
        {
            MascotaId = r.MascotaId,
            ServicioId = r.ServicioId,
            PrecioServicioHistorico = servicio.Precio,
            VeterinarioId = r.VeterinarioId,
            Fecha = r.Fecha,
            Hora = hora,
        };
        citas.Agregar(cita);
        await uow.GuardarAsync(ct);
        return mapeador.Cita(await Cargar(cita.Id, ct));
    }

    public async Task<Pagina<CitaDto>> ListarAsync(Actor actor, FiltroCitas filtro, CancellationToken ct)
    {
        var final = filtro with
        {
            DuenoId = actor.Rol == Roles.Dueno ? actor.Id : null,
            VeterinarioId = actor.Rol == Roles.Veterinario ? actor.Id : null,
        };
        var (items, total) = await citas.PaginarAsync(final, ct);
        return new Pagina<CitaDto>(items.Select(c => mapeador.Cita(c)).ToList(), Paginacion.Crear(filtro.Page, filtro.Limit, total));
    }

    public async Task<CitaDto> ObtenerAsync(Actor actor, int id, CancellationToken ct)
    {
        var cita = await Cargar(id, ct);
        var esSuya = (actor.Rol == Roles.Dueno && cita.Mascota.DuenoId == actor.Id)
            || (actor.Rol == Roles.Veterinario && cita.VeterinarioId == actor.Id);
        if (actor.Rol != Roles.Administrador && !esSuya) throw new ProhibidoException("No tienes acceso a esta cita");
        return mapeador.Cita(cita);
    }

    public async Task<CitaDto> CancelarAsync(Actor actor, int id, CancellationToken ct)
    {
        var cita = await Cargar(id, ct);
        if (cita.Mascota.DuenoId != actor.Id) throw new ProhibidoException("Solo puedes cancelar tus propias citas");
        if (!Reglas.PuedeCancelar(cita.Estado)) throw new ConflictoException("La cita ya no puede cancelarse");
        cita.Estado = EstadoCita.CANCELADA;
        await uow.GuardarAsync(ct);
        return mapeador.Cita(cita);
    }

    public async Task<CitaDto> IniciarConsultaAsync(Actor actor, int citaId, CancellationToken ct)
    {
        var cita = await Cargar(citaId, ct);
        if (cita.VeterinarioId != actor.Id)
            throw new ProhibidoException("Solo el veterinario asignado puede iniciar esta consulta");
        if (cita.Estado == EstadoCita.EN_CONSULTA) return mapeador.Cita(cita);
        if (cita.Estado is not (EstadoCita.PENDIENTE or EstadoCita.CONFIRMADA))
            throw new ConflictoException("Esta cita ya fue atendida o cancelada");
        cita.Estado = EstadoCita.EN_CONSULTA;
        await uow.GuardarAsync(ct);
        return mapeador.Cita(cita);
    }

    public async Task<ConsultaDto> RegistrarConsultaAsync(Actor actor, int citaId, ConsultaRequest r, CancellationToken ct)
    {
        var cita = await Cargar(citaId, ct);
        if (cita.VeterinarioId != actor.Id)
            throw new ProhibidoException("Solo el veterinario asignado puede registrar esta consulta");
        if (!Reglas.PuedeRegistrarConsulta(cita.Estado)) throw new ConflictoException("Esta cita ya fue atendida o cancelada");

        var consulta = new Consulta
        {
            CitaId = cita.Id,
            MascotaId = cita.MascotaId,
            VeterinarioId = actor.Id,
            Fecha = cita.Fecha,
            Diagnostico = r.Diagnostico,
            Tratamiento = r.Tratamiento,
            Observaciones = r.Observaciones,
            PesoKg = r.Peso,
        };
        cita.Estado = EstadoCita.ATENDIDA;
        citas.AgregarConsulta(consulta);
        await uow.GuardarAsync(ct);
        consulta.Veterinario = cita.Veterinario;
        return mapeador.Consulta(consulta);
    }

    public async Task<ConsultaDto> ActualizarObservacionesAsync(Actor actor, int citaId, string observaciones, CancellationToken ct)
    {
        var cita = await Cargar(citaId, ct);
        var consulta = await citas.ConsultaPorCitaAsync(citaId, ct);
        if (consulta is not null)
        {
            consulta.Observaciones = observaciones?.Trim();
            await uow.GuardarAsync(ct);
            consulta.Veterinario ??= await veterinarios.PorIdAsync(consulta.VeterinarioId, ct) ?? cita.Veterinario;
            return mapeador.Consulta(consulta);
        }
        else
        {
            consulta = new Consulta
            {
                CitaId = cita.Id,
                MascotaId = cita.MascotaId,
                VeterinarioId = cita.VeterinarioId,
                Fecha = cita.Fecha,
                Diagnostico = "Consulta médica",
                Observaciones = observaciones?.Trim(),
            };
            if (cita.Estado == EstadoCita.PENDIENTE || cita.Estado == EstadoCita.CONFIRMADA || cita.Estado == EstadoCita.EN_CONSULTA)
            {
                cita.Estado = EstadoCita.ATENDIDA;
            }
            citas.AgregarConsulta(consulta);
            await uow.GuardarAsync(ct);
            consulta.Veterinario = cita.Veterinario;
            return mapeador.Consulta(consulta);
        }
    }

    private async Task<Cita> Cargar(int id, CancellationToken ct) =>
        await citas.PorIdAsync(id, ct) ?? throw new NoEncontradoException("Cita no encontrada");
}
