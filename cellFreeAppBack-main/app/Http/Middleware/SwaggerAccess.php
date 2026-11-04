<?php
namespace App\Http\Middleware;
use Closure;
use Illuminate\Http\Request;

class SwaggerAccess
{
    public function handle(Request $request, Closure $next)
    {
        abort_unless(app()->environment('local') || optional($request->user())->rol === 'SUPERADMIN', 404);
        return $next($request);
    }
}
