using Microsoft.EntityFrameworkCore;
using PetCare.Application.Abstracciones;
using PetCare.Application.Contratos;
using PetCare.Domain;

namespace PetCare.Infrastructure.Persistence;

public class UsuarioRepositorio(PetCareDbContext db) : IUsuarioRepositorio
{
    public Task<Usuario?> BuscarPorEmailAsync(string email, CancellationToken ct) =>
        db.Usuarios.Include(u => u.Rol).FirstOrDefaultAsync(u => u.Email == email, ct);

    public Task<Usuario?> BuscarPorIdAsync(int id, CancellationToken ct) =>
        db.Usuarios.Include(u => u.Rol).FirstOrDefaultAsync(u => u.Id == id, ct);

    public Task<int> IdRolAsync(string nombre, CancellationToken ct) =>
        db.Roles.Where(r => r.Nombre == nombre).Select(r => r.Id).SingleAsync(ct);

    public void Agregar(Usuario usuario) => db.Usuarios.Add(usuario);
}

public class RefreshTokenRepositorio(PetCareDbContext db) : IRefreshTokenRepositorio
{
    public void Agregar(RefreshToken token) => db.RefreshTokens.Add(token);

    public Task<RefreshToken?> BuscarPorHashAsync(string hash, CancellationToken ct) =>
        db.RefreshTokens.FirstOrDefaultAsync(t => t.TokenHash == hash, ct);

    public Task RevocarTodosAsync(int usuarioId, CancellationToken ct) =>
        db.RefreshTokens.Where(t => t.UsuarioId == usuarioId && !t.Revocado)
            .ExecuteUpdateAsync(s => s.SetProperty(t => t.Revocado, true), ct);
}

public class CatalogoRepositorio(PetCareDbContext db) : ICatalogoRepositorio
{
    public async Task<IReadOnlyList<Especie>> EspeciesAsync(CancellationToken ct) =>
        await db.Especies.OrderBy(e => e.Nombre).ToListAsync(ct);

    public Task<bool> EspecieExisteAsync(int id, CancellationToken ct) =>
        db.Especies.AnyAsync(e => e.Id == id, ct);

    public async Task<IReadOnlyList<Vacuna>> VacunasAsync(CancellationToken ct) =>
        await db.Vacunas.OrderBy(v => v.Nombre).ToListAsync(ct);

    public Task<Vacuna?> VacunaPorIdAsync(int id, CancellationToken ct) =>
        db.Vacunas.FirstOrDefaultAsync(v => v.Id == id, ct);

    public async Task<IReadOnlyList<Servicio>> ServiciosAsync(bool soloActivos, CancellationToken ct) =>
        await db.Servicios.Where(s => !soloActivos || s.Activo).OrderBy(s => s.Nombre).ToListAsync(ct);

    public Task<Servicio?> ServicioPorIdAsync(int id, CancellationToken ct) =>
        db.Servicios.FirstOrDefaultAsync(s => s.Id == id, ct);

    public void AgregarServicio(Servicio servicio) => db.Servicios.Add(servicio);
}

public class VeterinarioRepositorio(PetCareDbContext db) : IVeterinarioRepositorio
{
    public async Task<IReadOnlyList<Usuario>> ListarAsync(CancellationToken ct) =>
        await db.Usuarios.Include(u => u.Rol).Include(u => u.Horarios)
            .Where(u => u.Rol.Nombre == Roles.Veterinario && u.Activo)
            .OrderBy(u => u.Nombre).ToListAsync(ct);

    public Task<Usuario?> PorIdAsync(int id, CancellationToken ct) =>
        db.Usuarios.Include(u => u.Rol).Include(u => u.Horarios)
            .FirstOrDefaultAsync(u => u.Id == id && u.Rol.Nombre == Roles.Veterinario && u.Activo, ct);
}

public class MascotaRepositorio(PetCareDbContext db) : IMascotaRepositorio
{
    public async Task<(IReadOnlyList<Mascota> Items, int Total)> PaginarAsync(int? duenoId, int page, int limit, CancellationToken ct)
    {
        var consulta = db.Mascotas.Include(m => m.Especie).Include(m => m.Dueno).Where(m => m.Activo);
        if (duenoId is not null) consulta = consulta.Where(m => m.DuenoId == duenoId);
        var total = await consulta.CountAsync(ct);
        var items = await consulta.OrderBy(m => m.Nombre).Skip((page - 1) * limit).Take(limit).ToListAsync(ct);
        return (items, total);
    }

    public Task<Mascota?> PorIdAsync(int id, CancellationToken ct) =>
        db.Mascotas.Include(m => m.Especie).Include(m => m.Dueno).FirstOrDefaultAsync(m => m.Id == id, ct);

    public void Agregar(Mascota mascota) => db.Mascotas.Add(mascota);
}

public class VacunacionRepositorio(PetCareDbContext db) : IVacunacionRepositorio
{
    public async Task<IReadOnlyList<AplicacionVacuna>> DeMascotaAsync(int mascotaId, CancellationToken ct) =>
        await db.AplicacionesVacuna.Include(a => a.Vacuna).Include(a => a.Veterinario)
            .Where(a => a.MascotaId == mascotaId)
            .OrderByDescending(a => a.FechaAplicacion).ToListAsync(ct);

    public Task<AplicacionVacuna?> PorIdAsync(int id, CancellationToken ct) =>
        db.AplicacionesVacuna.Include(a => a.Vacuna).Include(a => a.Veterinario)
            .FirstOrDefaultAsync(a => a.Id == id, ct);

    public void Agregar(AplicacionVacuna aplicacion) => db.AplicacionesVacuna.Add(aplicacion);

    public async Task<IReadOnlyList<AplicacionVacuna>> PendientesRecordatorioAsync(DateOnly desde, DateOnly hasta, CancellationToken ct) =>
        await db.AplicacionesVacuna.Include(a => a.Mascota).Include(a => a.Vacuna)
            .Where(a => !a.RecordatorioEnviado && a.Mascota.Activo
                && a.ProximaDosis >= desde && a.ProximaDosis <= hasta)
            .ToListAsync(ct);
}

public class CitaRepositorio(PetCareDbContext db) : ICitaRepositorio
{
    private IQueryable<Cita> Completa() =>
        db.Citas.Include(c => c.Mascota).ThenInclude(m => m.Dueno)
            .Include(c => c.Servicio).Include(c => c.Veterinario);

    public Task<Cita?> PorIdAsync(int id, CancellationToken ct) =>
        Completa().FirstOrDefaultAsync(c => c.Id == id, ct);

    public async Task<(IReadOnlyList<Cita> Items, int Total)> PaginarAsync(FiltroCitas f, CancellationToken ct)
    {
        var consulta = Completa().AsQueryable();
        if (f.DuenoId is not null) consulta = consulta.Where(c => c.Mascota.DuenoId == f.DuenoId);
        if (f.VeterinarioId is not null) consulta = consulta.Where(c => c.VeterinarioId == f.VeterinarioId);
        if (f.MascotaId is not null) consulta = consulta.Where(c => c.MascotaId == f.MascotaId);
        if (f.Fecha is not null) consulta = consulta.Where(c => c.Fecha == f.Fecha);
        if (f.Estado is not null) consulta = consulta.Where(c => c.Estado == f.Estado);

        var total = await consulta.CountAsync(ct);
        var items = await consulta.OrderBy(c => c.Fecha).ThenBy(c => c.Hora)
            .Skip((f.Page - 1) * f.Limit).Take(f.Limit).ToListAsync(ct);
        return (items, total);
    }

    public async Task<IReadOnlyList<Cita>> DeMascotaAsync(int mascotaId, CancellationToken ct) =>
        await Completa()
            .Where(c => c.MascotaId == mascotaId)
            .OrderByDescending(c => c.Fecha).ThenByDescending(c => c.Hora)
            .ToListAsync(ct);

    public async Task<IReadOnlyList<Ocupacion>> ActivasPorVeterinarioYFechaAsync(int veterinarioId, DateOnly fecha, CancellationToken ct)
    {
        var filas = await db.Citas
            .Where(c => c.VeterinarioId == veterinarioId && c.Fecha == fecha
                && (c.Estado == EstadoCita.PENDIENTE || c.Estado == EstadoCita.CONFIRMADA
                    || c.Estado == EstadoCita.EN_CONSULTA || c.Estado == EstadoCita.ATENDIDA))
            .Select(c => new { c.Hora, Duracion = c.Servicio.DuracionMin })
            .ToListAsync(ct);
        return filas.Select(f => new Ocupacion(f.Hora, f.Duracion)).ToList();
    }

    public async Task<IReadOnlyList<Consulta>> ConsultasDeMascotaAsync(int mascotaId, CancellationToken ct) =>
        await db.Consultas.Include(c => c.Veterinario)
            .Where(c => c.MascotaId == mascotaId)
            .OrderByDescending(c => c.Fecha).ToListAsync(ct);

    public async Task<Consulta?> ConsultaPorCitaAsync(int citaId, CancellationToken ct) =>
        await db.Consultas.Include(c => c.Veterinario)
            .FirstOrDefaultAsync(c => c.CitaId == citaId, ct);

    public async Task<IReadOnlyList<Cita>> PendientesRecordatorioAsync(DateOnly fecha, CancellationToken ct) =>
        await Completa().Where(c => c.Fecha == fecha
            && (c.Estado == EstadoCita.PENDIENTE || c.Estado == EstadoCita.CONFIRMADA)
            && !c.RecordatorioEnviado).ToListAsync(ct);

    public void Agregar(Cita cita) => db.Citas.Add(cita);

    public void AgregarConsulta(Consulta consulta) => db.Consultas.Add(consulta);
}

public class ProductoRepositorio(PetCareDbContext db) : IProductoRepositorio
{
    public async Task<(IReadOnlyList<Producto> Items, int Total)> PaginarAsync(string? categoria, bool soloActivos, int page, int limit, CancellationToken ct)
    {
        var consulta = db.Productos.Where(p => !soloActivos || p.Activo);
        if (!string.IsNullOrWhiteSpace(categoria)) consulta = consulta.Where(p => p.Categoria == categoria);
        var total = await consulta.CountAsync(ct);
        var items = await consulta.OrderBy(p => p.Nombre).Skip((page - 1) * limit).Take(limit).ToListAsync(ct);
        return (items, total);
    }

    public Task<Producto?> PorIdAsync(int id, CancellationToken ct) =>
        db.Productos.FirstOrDefaultAsync(p => p.Id == id, ct);

    public async Task<IReadOnlyList<Producto>> PorIdsAsync(IReadOnlyCollection<int> ids, CancellationToken ct) =>
        await db.Productos.Where(p => ids.Contains(p.Id)).ToListAsync(ct);

    public void Agregar(Producto producto) => db.Productos.Add(producto);
}

public class PedidoRepositorio(PetCareDbContext db) : IPedidoRepositorio
{
    public async Task<(IReadOnlyList<Pedido> Items, int Total)> PaginarAsync(int? clienteId, int page, int limit, CancellationToken ct)
    {
        var consulta = db.Pedidos.Include(p => p.Cliente).Include(p => p.Items).AsSplitQuery().AsQueryable();
        if (clienteId is not null) consulta = consulta.Where(p => p.ClienteId == clienteId);
        var total = await consulta.CountAsync(ct);
        var items = await consulta.OrderByDescending(p => p.Fecha).ThenByDescending(p => p.Id)
            .Skip((page - 1) * limit).Take(limit).ToListAsync(ct);
        return (items, total);
    }

    public Task<Pedido?> PorIdAsync(int id, CancellationToken ct) =>
        db.Pedidos.Include(p => p.Cliente).Include(p => p.Items).AsSplitQuery()
            .FirstOrDefaultAsync(p => p.Id == id, ct);

    public void Agregar(Pedido pedido) => db.Pedidos.Add(pedido);
}

public class NotificacionRepositorio(PetCareDbContext db) : INotificacionRepositorio
{
    public async Task<IReadOnlyList<Notificacion>> DeUsuarioAsync(int usuarioId, CancellationToken ct) =>
        await db.Notificaciones.Where(n => n.UsuarioId == usuarioId)
            .OrderByDescending(n => n.CreatedAt).ThenByDescending(n => n.Id).Take(100).ToListAsync(ct);

    public Task<Notificacion?> DeUsuarioPorIdAsync(int id, int usuarioId, CancellationToken ct) =>
        db.Notificaciones.FirstOrDefaultAsync(n => n.Id == id && n.UsuarioId == usuarioId, ct);

    public void Agregar(Notificacion notificacion) => db.Notificaciones.Add(notificacion);

    public async Task<IReadOnlyList<string>> TokensDeUsuarioAsync(int usuarioId, CancellationToken ct) =>
        await db.Dispositivos.Where(d => d.UsuarioId == usuarioId).Select(d => d.FcmToken).ToListAsync(ct);

    public async Task GuardarDispositivoAsync(int usuarioId, string fcmToken, CancellationToken ct)
    {
        var existente = await db.Dispositivos.FirstOrDefaultAsync(d => d.FcmToken == fcmToken, ct);
        if (existente is null) db.Dispositivos.Add(new Dispositivo { UsuarioId = usuarioId, FcmToken = fcmToken });
        else existente.UsuarioId = usuarioId;
    }

    public async Task EliminarDispositivosAsync(IReadOnlyList<string> fcmTokens, CancellationToken ct)
    {
        foreach (var token in fcmTokens.Distinct())
        {
            var dispositivo = await db.Dispositivos.FirstOrDefaultAsync(d => d.FcmToken == token, ct);
            if (dispositivo is not null) db.Dispositivos.Remove(dispositivo);
        }
    }
}

public class ReporteRepositorio(PetCareDbContext db) : IReporteRepositorio
{
    public async Task<VentasResumen> VentasAsync(DateOnly desde, DateOnly hasta, CancellationToken ct)
    {
        var inicio = desde.ToDateTime(TimeOnly.MinValue);
        var fin = hasta.AddDays(1).ToDateTime(TimeOnly.MinValue);
        var pedidos = db.Pedidos.Where(p => p.Estado != EstadoPedido.CANCELADO && p.Fecha >= inicio && p.Fecha < fin);

        var totalPedidos = await pedidos.CountAsync(ct);
        var totalVentas = await pedidos.SumAsync(p => (decimal?)p.Total, ct) ?? 0m;

        var filas = await db.PedidoItems
            .Where(i => i.Pedido.Estado != EstadoPedido.CANCELADO && i.Pedido.Fecha >= inicio && i.Pedido.Fecha < fin)
            .GroupBy(i => new { i.ProductoId, i.NombreProducto })
            .Select(g => new { g.Key.ProductoId, g.Key.NombreProducto, Cantidad = g.Sum(x => x.Cantidad), Total = g.Sum(x => x.Subtotal) })
            .ToListAsync(ct);

        var productos = filas
            .OrderByDescending(f => f.Total)
            .Select(f => new VentaProductoDto(f.ProductoId, f.NombreProducto, f.Cantidad, f.Total))
            .ToList();
        return new VentasResumen(totalPedidos, Reglas.Redondear(totalVentas), productos);
    }

    public async Task<CitasResumen> CitasAsync(DateOnly desde, DateOnly hasta, CancellationToken ct)
    {
        var citas = db.Citas.Where(c => c.Fecha >= desde && c.Fecha <= hasta);

        var conteoEstado = await citas.GroupBy(c => c.Estado)
            .Select(g => new { Estado = g.Key, Cantidad = g.Count() }).ToListAsync(ct);
        var porEstado = Enum.GetValues<EstadoCita>().ToDictionary(e => e.ToString(), _ => 0);
        foreach (var fila in conteoEstado) porEstado[fila.Estado.ToString()] = fila.Cantidad;

        var conteoServicio = await citas.GroupBy(c => c.ServicioId)
            .Select(g => new { ServicioId = g.Key, Cantidad = g.Count() }).ToDictionaryAsync(x => x.ServicioId, x => x.Cantidad, ct);
        var servicios = await db.Servicios.ToListAsync(ct);
        var porServicio = servicios
            .Select(s => new CitasServicioDto(s.Id, s.Nombre, conteoServicio.GetValueOrDefault(s.Id)))
            .OrderByDescending(s => s.Cantidad).ThenBy(s => s.Nombre).ToList();

        return new CitasResumen(porEstado.Values.Sum(), porEstado, porServicio);
    }

}
