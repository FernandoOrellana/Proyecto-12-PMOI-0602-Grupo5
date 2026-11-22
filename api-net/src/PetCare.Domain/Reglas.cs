namespace PetCare.Domain;

public readonly record struct Franja(int DiaSemana, TimeOnly Inicio, TimeOnly Fin);

public readonly record struct Ocupacion(TimeOnly Hora, int DuracionMin);

public static class Reglas
{
    public static int Minutos(TimeOnly hora) => hora.Hour * 60 + hora.Minute;

    public static TimeOnly DesdeMinutos(int minutos) => new(minutos / 60, minutos % 60);

    public static bool Solapan(TimeOnly horaA, int duracionA, TimeOnly horaB, int duracionB)
    {
        var inicioA = Minutos(horaA);
        var inicioB = Minutos(horaB);
        return inicioA < inicioB + duracionB && inicioB < inicioA + duracionA;
    }

    public static bool EsFechaHoraFutura(DateOnly fecha, TimeOnly hora, DateTime ahora)
    {
        var hoy = DateOnly.FromDateTime(ahora);
        if (fecha > hoy) return true;
        if (fecha < hoy) return false;
        return Minutos(hora) > ahora.Hour * 60 + ahora.Minute;
    }

    public static IReadOnlyList<TimeOnly> HorasDisponibles(
        DateOnly fecha,
        IEnumerable<Franja> horarios,
        IEnumerable<Ocupacion> ocupadas,
        int duracionMin,
        DateTime ahora)
    {
        var diaSemana = (int)fecha.DayOfWeek;
        var esHoy = fecha == DateOnly.FromDateTime(ahora);
        var minutosAhora = ahora.Hour * 60 + ahora.Minute;
        var ocupadasLista = ocupadas.ToList();
        var horas = new SortedSet<TimeOnly>();

        foreach (var franja in horarios.Where(h => h.DiaSemana == diaSemana))
        {
            var fin = Minutos(franja.Fin);
            for (var t = Minutos(franja.Inicio); t + duracionMin <= fin; t += Parametros.SlotMinutos)
            {
                if (esHoy && t <= minutosAhora) continue;
                var hora = DesdeMinutos(t);
                if (ocupadasLista.Any(o => Solapan(hora, duracionMin, o.Hora, o.DuracionMin))) continue;
                horas.Add(hora);
            }
        }
        return horas.ToList();
    }

    public static bool PuedeCancelar(EstadoCita estado) =>
        estado is EstadoCita.PENDIENTE or EstadoCita.CONFIRMADA;

    public static bool PuedeRegistrarConsulta(EstadoCita estado) =>
        estado is EstadoCita.PENDIENTE or EstadoCita.CONFIRMADA or EstadoCita.EN_CONSULTA;

    public static bool TransicionPedidoValida(EstadoPedido desde, EstadoPedido hasta) => (desde, hasta) switch
    {
        (EstadoPedido.PENDIENTE, EstadoPedido.LISTO_PARA_RECOGER) => true,
        (EstadoPedido.PENDIENTE, EstadoPedido.CANCELADO) => true,
        (EstadoPedido.LISTO_PARA_RECOGER, EstadoPedido.ENTREGADO) => true,
        (EstadoPedido.LISTO_PARA_RECOGER, EstadoPedido.CANCELADO) => true,
        _ => false,
    };

    public static decimal Redondear(decimal valor) => Math.Round(valor, 2, MidpointRounding.AwayFromZero);

    public static decimal Total(IEnumerable<decimal> subtotales) => Redondear(subtotales.Sum());

    public static DateOnly ProximaDosis(DateOnly fechaAplicacion, int intervaloDias) =>
        fechaAplicacion.AddDays(intervaloDias);

    public static IReadOnlyList<T> ProximasDosis<T>(
        IEnumerable<T> aplicaciones,
        Func<T, int> vacunaId,
        Func<T, DateOnly> fechaAplicacion,
        Func<T, DateOnly?> proximaDosis)
    {
        var ultimaPorVacuna = new Dictionary<int, T>();
        foreach (var a in aplicaciones)
        {
            if (!ultimaPorVacuna.TryGetValue(vacunaId(a), out var previa) || fechaAplicacion(a) > fechaAplicacion(previa))
                ultimaPorVacuna[vacunaId(a)] = a;
        }
        return ultimaPorVacuna.Values
            .Where(a => proximaDosis(a) is not null)
            .OrderBy(a => proximaDosis(a))
            .ToList();
    }
}
