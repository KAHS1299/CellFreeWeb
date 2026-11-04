<?php
namespace App\Http\Middleware;
use Closure;
use Illuminate\Http\Request;

class AdminOnly
{
    public function handle(Request $request, Closure $next)
    {
        abort_unless($request->user() && in_array($request->user()->rol, ['SUPERADMIN', 'ADMINISTRADOR'], true), 403);
        abort_unless($request->user()->activo, 403);
        return $next($request);
    }
}
