using Moq;
using PetCare.Application.Abstracciones;
using PetCare.Application.Common;
using PetCare.Application.Contratos;
using PetCare.Application.Servicios;
using PetCare.Domain;

namespace PetCare.Tests.Unitarias;

public class TiendaServiceTests
{
    private readonly Mock<IProductoRepositorio> _productos = new();
    private readonly Mock<IPedidoRepositorio> _pedidos = new();
    private readonly Mock<ICobroRepositorio> _cobros = new();
    private readonly Mock<IUnitOfWork> _uow = new();

    private TiendaService Crear() => new(_productos.Object, _pedidos.Object, _cobros.Object, _uow.Object,
        Mocks.Reloj(new DateTime(2030, 1, 1, 9, 0, 0)).Object, Mocks.Mapeador());

    private static Producto Collar(int stock) =>
        new() { Id = 1, Nombre = "Collar", Precio = 150.50m, Stock = stock, Activo = true };

    [Fact]
    public async Task CrearPedido_CalculaElTotalEnElServidorYDescuentaStock()
    {
        var collar = Collar(stock: 5);
        _productos.Setup(p => p.PorIdsAsync(It.IsAny<IReadOnlyCollection<int>>(), It.IsAny<CancellationToken>())).ReturnsAsync([collar]);
        _pedidos.Setup(p => p.PorIdAsync(It.IsAny<int>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Pedido { Id = 1, Cliente = Datos.Usuario(5, Roles.Dueno), Estado = EstadoPedido.PENDIENTE, Fecha = DateTime.Now });

        await Crear().CrearPedidoAsync(new Actor(5, Roles.Dueno),
            new PedidoRequest([new ItemPedidoRequest(1, 2), new ItemPedidoRequest(1, 1)]), default);

        Assert.Equal(2, collar.Stock);
        _pedidos.Verify(p => p.Agregar(It.Is<Pedido>(x => x.Total == 451.50m && x.Items.Single().Cantidad == 3)), Times.Once);
    }

    [Fact]
    public async Task CrearPedido_RechazaUnPedidoQueSuperaElStock()
    {
        _productos.Setup(p => p.PorIdsAsync(It.IsAny<IReadOnlyCollection<int>>(), It.IsAny<CancellationToken>())).ReturnsAsync([Collar(stock: 1)]);

        await Assert.ThrowsAsync<ValidacionException>(() => Crear().CrearPedidoAsync(new Actor(5, Roles.Dueno),
            new PedidoRequest([new ItemPedidoRequest(1, 3)]), default));
        _pedidos.Verify(p => p.Agregar(It.IsAny<Pedido>()), Times.Never);
    }

    [Fact]
    public async Task CambiarEstado_RechazaUnaTransicionNoPermitida()
    {
        _pedidos.Setup(p => p.PorIdAsync(1, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Pedido { Id = 1, Cliente = Datos.Usuario(5, Roles.Dueno), Estado = EstadoPedido.ENTREGADO, Fecha = DateTime.Now });

        await Assert.ThrowsAsync<ValidacionException>(() => Crear().CambiarEstadoAsync(1, EstadoPedido.CANCELADO, default));
    }

    [Fact]
    public async Task CambiarEstado_AEntregado_RechazaSiNoTieneCobro()
    {
        _pedidos.Setup(p => p.PorIdAsync(1, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Pedido { Id = 1, Cliente = Datos.Usuario(5, Roles.Dueno), Estado = EstadoPedido.LISTO_PARA_RECOGER, Fecha = DateTime.Now });
        _cobros.Setup(c => c.PorOrigenAsync(true, 1, It.IsAny<CancellationToken>()))
            .ReturnsAsync((Cobro?)null);

        await Assert.ThrowsAsync<ConflictoException>(() => Crear().CambiarEstadoAsync(1, EstadoPedido.ENTREGADO, default));
    }

    [Fact]
    public async Task CambiarEstado_AEntregado_PermiteSiTieneCobro()
    {
        _pedidos.Setup(p => p.PorIdAsync(1, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Pedido { Id = 1, Cliente = Datos.Usuario(5, Roles.Dueno), Estado = EstadoPedido.LISTO_PARA_RECOGER, Fecha = DateTime.Now });
        _cobros.Setup(c => c.PorOrigenAsync(true, 1, It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Cobro { Id = 10, PedidoId = 1 });

        var resultado = await Crear().CambiarEstadoAsync(1, EstadoPedido.ENTREGADO, default);

        Assert.Equal("ENTREGADO", resultado.Estado);
    }
}
