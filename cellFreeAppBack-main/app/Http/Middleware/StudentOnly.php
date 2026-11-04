<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;

class StudentOnly
{
    public function handle(Request $request, Closure $next)
    {
        $user = $request->user();
        abort_unless($user && $user->activo && $user->rol === 'ESTUDIANTE', 403, 'Acceso exclusivo para estudiantes activos.');

        return $next($request);
    }
}
