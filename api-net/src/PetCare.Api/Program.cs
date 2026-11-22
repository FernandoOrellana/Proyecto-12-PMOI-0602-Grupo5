using System.Text;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.FileProviders;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using PetCare.Api.Infraestructura;
using PetCare.Application;
using PetCare.Application.Common;
using PetCare.Infrastructure;
using PetCare.Infrastructure.Persistence;
using Swashbuckle.AspNetCore.SwaggerGen;

var builder = WebApplication.CreateBuilder(args);
var configuracion = builder.Configuration;
var carpetaUploads = Path.GetFullPath(configuracion["Almacenamiento:Carpeta"] ?? "uploads");

builder.Services.AddInfrastructure(configuracion);
builder.Services.AddApplication();

builder.Services.AddControllers().AddJsonOptions(o =>
    o.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));

builder.Services.AddExceptionHandler<ManejadorErrores>();
builder.Services.AddSingleton<IAuthorizationMiddlewareResultHandler, SinPermisos>();

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(o =>
    {
        o.MapInboundClaims = false;
        var secreto = configuracion["Jwt:Secret"] ?? throw new InvalidOperationException("Falta Jwt:Secret");
        o.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = false,
            ValidateAudience = false,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secreto)),
            RoleClaimType = "rol",
            NameClaimType = "sub",
        };
        o.Events = new JwtBearerEvents
        {
            OnChallenge = async ctx =>
            {
                ctx.HandleResponse();
                ctx.Response.StatusCode = StatusCodes.Status401Unauthorized;
                await ctx.Response.WriteAsJsonAsync(new ErrorRespuestaDto(false,
                    new ErrorCuerpoDto("UNAUTHORIZED", "Token requerido o vencido", null)));
            },
        };
    });
builder.Services.AddAuthorization();

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new Microsoft.OpenApi.OpenApiInfo
    {
        Title = "PetCare HN - API Veterinaria Huellitas",
        Version = "1.0.0",
        Description = "API REST para mascotas, carné de vacunación, citas, tienda y administración. Envíe Authorization: Bearer <accessToken>.",
    });
    c.AddSecurityDefinition("bearerAuth", new OpenApiSecurityScheme
    {
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Token de acceso obtenido en /api/auth/login",
    });
    c.OperationFilter<SeguridadOperacionFiltro>();
});

Directory.CreateDirectory(carpetaUploads);
var app = builder.Build();

if (args.Length > 0 && args[0] is "migrate" or "seed")
{
    using var scope = app.Services.CreateScope();
    var db = scope.ServiceProvider.GetRequiredService<PetCareDbContext>();
    await Migrador.MigrarAsync(db);
    if (args[0] == "seed")
    {
        await scope.ServiceProvider.GetRequiredService<Seed>().EjecutarAsync(
            configuracion["Seed:AdminEmail"] ?? "admin@huellitas.hn",
            configuracion["Seed:AdminPassword"] ?? "",
            configuracion["Seed:DemoPassword"] ?? "Demo12345",
            CancellationToken.None);
    }
    return;
}

app.UseExceptionHandler(_ => { });
app.UseStatusCodePages(async ctx =>
{
    var respuesta = ctx.HttpContext.Response;
    if (respuesta.StatusCode == StatusCodes.Status404NotFound && !respuesta.HasStarted)
    {
        await respuesta.WriteAsJsonAsync(new ErrorRespuestaDto(false,
            new ErrorCuerpoDto("NOT_FOUND", $"Ruta no encontrada: {ctx.HttpContext.Request.Method} {ctx.HttpContext.Request.Path}", null)));
    }
});
app.UseStaticFiles(new StaticFileOptions
{
    FileProvider = new PhysicalFileProvider(carpetaUploads),
    RequestPath = "/uploads",
});
app.UseSwagger(o => o.RouteTemplate = "api-docs/{documentName}/swagger.json");
app.UseSwaggerUI(o =>
{
    o.RoutePrefix = "api-docs";
    o.SwaggerEndpoint("/api-docs/v1/swagger.json", "PetCare HN v1");
});
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
app.MapGet("/health", () => Results.Ok(new { success = true, data = new { status = "ok" } }));

app.Run();

public partial class Program;
