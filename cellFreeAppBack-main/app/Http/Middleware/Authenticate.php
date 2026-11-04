<?php

namespace App\Http\Middleware;

use Illuminate\Auth\Middleware\Authenticate as Middleware;

class Authenticate extends Middleware
{
    /**
     * Get the path the user should be redirected to when they are not authenticated.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return string|null
     */
    protected function redirectTo($request)
    {
        // Para todas las rutas de API, no redirigir
        if ($request->is('api/*')) {
            return null;
        }
        
        // Para rutas web que esperan JSON
        if ($request->expectsJson()) {
            return null;
        }
        
        // Solo para rutas web normales
        return route('login');
    }
}
