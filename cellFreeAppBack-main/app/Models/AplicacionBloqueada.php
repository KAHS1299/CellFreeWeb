<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class AplicacionBloqueada extends Model
{
    use HasFactory;

    protected $table = 'aplicaciones_bloqueadas';

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'usuario_id',
        'dispositivo_id',
        'administrador_id',
        'nombre_app',
        'categoria',
        'tiempo_bloqueo',
        'estado',
        'fecha_inicio',
        'fecha_fin',
        'nombre_paquete',
    ];

    /**
     * The attributes that should be cast.
     *
     * @var array<string, string>
     */
    protected $casts = [
        'estado' => 'boolean',
        'tiempo_bloqueo' => 'integer',
        'fecha_inicio' => 'datetime',
        'fecha_fin' => 'datetime',
    ];

    /**
     * Get the user that owns the blocked application.
     */
    public function usuario(): BelongsTo
    {
        return $this->belongsTo(User::class, 'usuario_id');
    }

    /**
     * Get the blocked application information for API responses
     */
    public function toApiArray(): array
    {
        return [
            'id' => $this->id,
            'nombre_app' => $this->nombre_app,
            'categoria' => $this->categoria,
            'tiempo_bloqueo' => $this->tiempo_bloqueo,
            'estado' => $this->estado,
            'fecha_inicio' => $this->fecha_inicio?->format('Y-m-d H:i:s'),
            'fecha_fin' => $this->fecha_fin?->format('Y-m-d H:i:s'),
            'nombre_paquete' => $this->nombre_paquete,
            'created_at' => $this->created_at,
            'updated_at' => $this->updated_at,
        ];
    }

    /**
     * Scope a query to only include active blocked applications.
     */
    public function scopeActivo($query)
    {
        return $query->where('estado', true);
    }

    /**
     * Scope a query to filter by category.
     */
    public function scopeCategoria($query, $categoria)
    {
        return $query->where('categoria', $categoria);
    }

    /**
     * Scope a query to only include applications currently within blocking period.
     */
    public function scopeActivoEnPeriodo($query)
    {
        $ahora = now();
        return $query->where('estado', true)
                    ->where(function ($q) use ($ahora) {
                        $q->where(function ($subQ) use ($ahora) {
                            // Si tiene fechas definidas, verificar que esté en el periodo
                            $subQ->whereNotNull('fecha_inicio')
                                 ->whereNotNull('fecha_fin')
                                 ->where('fecha_inicio', '<=', $ahora)
                                 ->where('fecha_fin', '>=', $ahora);
                        })->orWhere(function ($subQ) {
                            // Si no tiene fechas definidas, considerar activo siempre
                            $subQ->whereNull('fecha_inicio')
                                 ->whereNull('fecha_fin');
                        });
                    });
    }

    /**
     * Check if the application is currently blocked (considering time period).
     */
    public function estaActivoAhora(): bool
    {
        if (!$this->estado) {
            return false;
        }

        // Si no hay fechas definidas, está activo siempre que el estado sea true
        if (!$this->fecha_inicio && !$this->fecha_fin) {
            return true;
        }

        $ahora = now();
        $dentroDelPeriodo = true;
        
        // Verificar fecha de inicio
        if ($this->fecha_inicio) {
            $dentroDelPeriodo = $dentroDelPeriodo && ($ahora >= $this->fecha_inicio);
        }
        
        // Verificar fecha de fin
        if ($this->fecha_fin) {
            $dentroDelPeriodo = $dentroDelPeriodo && ($ahora <= $this->fecha_fin);
        }

        return $dentroDelPeriodo;
    }

    /**
     * Get remaining time for the block (in minutes).
     */
    public function tiempoRestante(): ?int
    {
        if (!$this->estaActivoAhora() || !$this->fecha_fin) {
            return null;
        }

        $minutos = now()->diffInMinutes($this->fecha_fin, false);
        return $minutos > 0 ? $minutos : 0;
    }
}
