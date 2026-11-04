<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class CalendarSync extends Model
{
    use HasFactory;

    protected $table = 'calendar_syncs';

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'usuario_id',
        'nombre',
        'url_calendario',
        'estado',
        'intervalo_sincronizacion',
        'ultima_sincronizacion',
        'ultimo_error',
        'configuracion',
    ];

    /**
     * The attributes that should be cast.
     *
     * @var array<string, string>
     */
    protected $casts = [
        'configuracion' => 'array',
        'ultima_sincronizacion' => 'datetime',
    ];

    /**
     * Relación con el modelo User (Usuario)
     */
    public function usuario(): BelongsTo
    {
        return $this->belongsTo(User::class, 'usuario_id');
    }

    /**
     * Scope para sincronizaciones activas
     */
    public function scopeActivas($query)
    {
        return $query->where('estado', 'activo');
    }

    /**
     * Scope para sincronizaciones que necesitan actualización
     */
    public function scopeParaSincronizar($query)
    {
        return $query->where('estado', 'activo')
                    ->where(function($q) {
                        $q->whereNull('ultima_sincronizacion')
                          ->orWhereRaw('ultima_sincronizacion < NOW() - INTERVAL intervalo_sincronizacion MINUTE');
                    });
    }

    /**
     * Marcar sincronización como completada
     */
    public function marcarSincronizada()
    {
        $this->update([
            'ultima_sincronizacion' => now(),
            'ultimo_error' => null,
            'estado' => 'activo'
        ]);
    }

    /**
     * Marcar sincronización con error
     */
    public function marcarError($mensaje)
    {
        $this->update([
            'ultimo_error' => $mensaje,
            'estado' => 'error'
        ]);
    }

    /**
     * Convertir para API response
     */
    public function toApiArray(): array
    {
        return [
            'id' => $this->id,
            'nombre' => $this->nombre,
            'url_calendario' => $this->url_calendario,
            'estado' => $this->estado,
            'intervalo_sincronizacion' => $this->intervalo_sincronizacion,
            'ultima_sincronizacion' => $this->ultima_sincronizacion?->toISOString(),
            'ultimo_error' => $this->ultimo_error,
            'configuracion' => $this->configuracion,
            'created_at' => $this->created_at->toISOString(),
            'updated_at' => $this->updated_at->toISOString(),
        ];
    }
}
