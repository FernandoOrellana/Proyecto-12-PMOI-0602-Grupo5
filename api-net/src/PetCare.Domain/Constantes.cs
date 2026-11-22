namespace PetCare.Domain;

public static class Roles
{
    public const string Dueno = "DUENO";
    public const string Veterinario = "VETERINARIO";
    public const string Administrador = "ADMINISTRADOR";
}

public enum EstadoCita { PENDIENTE, CONFIRMADA, EN_CONSULTA, ATENDIDA, CANCELADA }

public enum EstadoPedido { PENDIENTE, LISTO_PARA_RECOGER, ENTREGADO, CANCELADO }

public enum MetodoCobro { EFECTIVO, TRANSFERENCIA }

public static class Parametros
{
    public const int SlotMinutos = 30;
    public const int CodigoLongitud = 6;
}
