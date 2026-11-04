<?php

namespace App\Http\Requests\Api;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;
use Illuminate\Contracts\Validation\Validator;

class CreateAppBlockRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        return true;
    }

    /**
     * Get the validation rules that apply to the request.
     */
    public function rules(): array
    {
        return [
            'nombre_app' => 'required|string|max:100',
            'categoria' => 'nullable|string|max:50|in:social,juegos,entretenimiento,productividad,otros',
            'tiempo_bloqueo' => 'nullable|integer|min:1|max:1440', // máximo 24 horas en minutos
            'estado' => 'boolean',
        ];
    }

    /**
     * Get the error messages for the defined validation rules.
     */
    public function messages(): array
    {
        return [
            'nombre_app.required' => 'El nombre de la aplicación es obligatorio.',
            'nombre_app.string' => 'El nombre de la aplicación debe ser una cadena de texto.',
            'nombre_app.max' => 'El nombre de la aplicación no debe exceder los 100 caracteres.',
            'categoria.string' => 'La categoría debe ser una cadena de texto.',
            'categoria.max' => 'La categoría no debe exceder los 50 caracteres.',
            'categoria.in' => 'La categoría debe ser: social, juegos, entretenimiento, productividad u otros.',
            'tiempo_bloqueo.integer' => 'El tiempo de bloqueo debe ser un número entero.',
            'tiempo_bloqueo.min' => 'El tiempo de bloqueo debe ser al menos 1 minuto.',
            'tiempo_bloqueo.max' => 'El tiempo de bloqueo no puede exceder 1440 minutos (24 horas).',
            'estado.boolean' => 'El estado debe ser verdadero o falso.',
        ];
    }

    /**
     * Handle a failed validation attempt.
     */
    protected function failedValidation(Validator $validator)
    {
        throw new HttpResponseException(
            response()->json([
                'status' => 'error',
                'message' => 'Datos de validación incorrectos',
                'errors' => $validator->errors()
            ], 422)
        );
    }
}
