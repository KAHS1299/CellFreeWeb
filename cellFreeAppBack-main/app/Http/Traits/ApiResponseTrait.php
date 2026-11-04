<?php

namespace App\Http\Traits;

use Illuminate\Http\JsonResponse;

trait ApiResponseTrait
{
    /**
     * Success response method.
     */
    protected function successResponse($data = null, string $message = 'Operación exitosa', int $code = 200): JsonResponse
    {
        return response()->json([
            'status' => 'success',
            'message' => $message,
            'data' => $data
        ], $code);
    }

    /**
     * Error response method.
     */
    protected function errorResponse(string $message = 'Ha ocurrido un error', $errors = null, int $code = 400): JsonResponse
    {
        $response = [
            'status' => 'error',
            'message' => $message,
        ];

        if ($errors) {
            $response['errors'] = $errors;
        }

        return response()->json($response, $code);
    }

    /**
     * Validation error response method.
     */
    protected function validationErrorResponse($errors, string $message = 'Datos de validación incorrectos'): JsonResponse
    {
        return $this->errorResponse($message, $errors, 422);
    }

    /**
     * Unauthorized response method.
     */
    protected function unauthorizedResponse(string $message = 'No autorizado'): JsonResponse
    {
        return $this->errorResponse($message, null, 401);
    }

    /**
     * Not found response method.
     */
    protected function notFoundResponse(string $message = 'Recurso no encontrado'): JsonResponse
    {
        return $this->errorResponse($message, null, 404);
    }

    /**
     * Server error response method.
     */
    protected function serverErrorResponse(string $message = 'Error interno del servidor'): JsonResponse
    {
        return $this->errorResponse($message, null, 500);
    }
}
