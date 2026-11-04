<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Http\JsonResponse;
use Symfony\Component\HttpFoundation\Response;

class ApiResponseMiddleware
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $response = $next($request);

        // Only process JSON responses for API routes
        if ($request->is('api/*') && $response instanceof JsonResponse) {
            $data = $response->getData(true);
            
            // Ensure consistent response format
            if (!isset($data['status'])) {
                $statusCode = $response->getStatusCode();
                
                if ($statusCode >= 200 && $statusCode < 300) {
                    $data = [
                        'status' => 'success',
                        'data' => $data
                    ];
                } else {
                    $data = [
                        'status' => 'error',
                        'message' => $data['message'] ?? 'Ha ocurrido un error',
                        'errors' => $data['errors'] ?? null
                    ];
                }
                
                $response->setData($data);
            }
        }

        return $response;
    }
}
