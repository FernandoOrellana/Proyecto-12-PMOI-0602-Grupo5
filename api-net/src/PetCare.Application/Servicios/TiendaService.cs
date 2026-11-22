using PetCare.Application.Abstracciones;
using PetCare.Application.Common;
using PetCare.Application.Contratos;
using PetCare.Domain;

namespace PetCare.Application.Servicios;

public class TiendaService(
    IProductoRepositorio productos,
    IPedidoRepositorio pedidos,
    ICobroRepositorio cobros,
    IUnitOfWork uow,
    IReloj reloj,
    Mapeador mapeador)
{
    public async Task<Pagina<ProductoDto>> ListarProductosAsync(string? categoria, int page, int limit, CancellationToken ct)
    {
        var (items, total) = await productos.PaginarAsync(categoria, true, page, limit, ct);
        return new Pagina<ProductoDto>(items.Select(mapeador.Producto).ToList(), Paginacion.Crear(page, limit, total));
    }

    public async Task<ProductoDto> ObtenerProductoAsync(int id, CancellationToken ct)
    {
        var producto = await productos.PorIdAsync(id, ct);
        if (producto is null || !producto.Activo) throw new NoEncontradoException("Producto no encontrado");
        return mapeador.Producto(producto);
    }

    public async Task<PedidoDto> CrearPedidoAsync(Actor actor, PedidoRequest r, CancellationToken ct)
    {
        var cantidades = r.Items
            .GroupBy(i => i.ProductoId)
            .ToDictionary(g => g.Key, g => g.Sum(i => i.Cantidad));

        var encontrados = (await productos.PorIdsAsync(cantidades.Keys.ToList(), ct)).ToDictionary(p => p.Id);

        var pedido = new Pedido { ClienteId = actor.Id, Fecha = reloj.Ahora };
        foreach (var (productoId, cantidad) in cantidades)
        {
            if (!encontrados.TryGetValue(productoId, out var producto) || !producto.Activo)
                throw new ValidacionException($"El producto {productoId} no está disponible");
            if (producto.Stock < cantidad)
                throw new ValidacionException($"Stock insuficiente para {producto.Nombre}",
                    [new ErrorDetalle("items", $"Disponibles: {producto.Stock}")]);

            producto.Stock -= cantidad;
            pedido.Items.Add(new PedidoItem
            {
                ProductoId = producto.Id,
                NombreProducto = producto.Nombre,
                PrecioUnitario = producto.Precio,
                Cantidad = cantidad,
                Subtotal = Reglas.Redondear(producto.Precio * cantidad),
            });
        }
        pedido.Total = Reglas.Total(pedido.Items.Select(i => i.Subtotal));
        pedidos.Agregar(pedido);
        await uow.GuardarAsync(ct);
        return mapeador.Pedido(await Cargar(pedido.Id, ct));
    }

    public async Task<Pagina<PedidoDto>> ListarPedidosAsync(Actor actor, int page, int limit, CancellationToken ct)
    {
        var (items, total) = await pedidos.PaginarAsync(actor.Rol == Roles.Dueno ? actor.Id : null, page, limit, ct);
        return new Pagina<PedidoDto>(items.Select(mapeador.Pedido).ToList(), Paginacion.Crear(page, limit, total));
    }

    public async Task<PedidoDto> ObtenerPedidoAsync(Actor actor, int id, CancellationToken ct)
    {
        var pedido = await Cargar(id, ct);
        if (actor.Rol == Roles.Dueno && pedido.ClienteId != actor.Id)
            throw new ProhibidoException("Este pedido no pertenece a tu cuenta");
        return mapeador.Pedido(pedido);
    }

    public async Task<PedidoDto> CambiarEstadoAsync(int id, EstadoPedido nuevo, CancellationToken ct)
    {
        var pedido = await Cargar(id, ct);
        if (!Reglas.TransicionPedidoValida(pedido.Estado, nuevo))
            throw new ValidacionException($"No se puede cambiar el pedido de {pedido.Estado} a {nuevo}");

        if (nuevo == EstadoPedido.ENTREGADO)
        {
            var cobro = await cobros.PorOrigenAsync(esPedido: true, id, ct);
            if (cobro is null)
                throw new ConflictoException("No se puede marcar el pedido como entregado sin haber registrado el cobro");
        }

        if (nuevo == EstadoPedido.CANCELADO)
        {
            var ids = pedido.Items.Select(i => i.ProductoId).ToList();
            var devueltos = (await productos.PorIdsAsync(ids, ct)).ToDictionary(p => p.Id);
            foreach (var item in pedido.Items)
                devueltos[item.ProductoId].Stock += item.Cantidad;
        }
        pedido.Estado = nuevo;
        await uow.GuardarAsync(ct);
        return mapeador.Pedido(pedido);
    }

    private async Task<Pedido> Cargar(int id, CancellationToken ct) =>
        await pedidos.PorIdAsync(id, ct) ?? throw new NoEncontradoException("Pedido no encontrado");
}
