using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi;
using Swashbuckle.AspNetCore.SwaggerGen;

namespace PetCare.Api.Infraestructura;

public class SeguridadOperacionFiltro : IOperationFilter
{
    public void Apply(OpenApiOperation operation, OperationFilterContext context)
    {
        var atributos = context.MethodInfo.GetCustomAttributes(true)
            .Concat(context.MethodInfo.DeclaringType?.GetCustomAttributes(true) ?? []);

        var requiereToken = atributos.OfType<AuthorizeAttribute>().Any()
            && !atributos.OfType<AllowAnonymousAttribute>().Any();
        if (!requiereToken) return;

        operation.Security = [new OpenApiSecurityRequirement
        {
            [new OpenApiSecuritySchemeReference("bearerAuth")] = [],
        }];
    }
}
