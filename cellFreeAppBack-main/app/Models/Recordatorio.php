<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Carbon\Carbon;

class Recordatorio extends Model
{
    use HasFactory;

    protected $table = 'recordatorios';

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'usuario_id',
        'titulo',
        'descripcion',
        'fecha_hora',
        'tipo',
        'completado',
        'calendario_sync_id',
        'uid_externo',
        'es_sincronizado',
    ];

    /**
     * The attributes that should be cast.
     *
     * @var array<string, string>
     */
    protected $casts = [
        'fecha_hora' => 'datetime',
        'completado' => 'boolean',
    ];

    /**
     * The attributes that should be hidden for serialization.
     *
     * @var array<int, string>
     */
    protected $hidden = [];

    /**
     * Get the user that owns the reminder.
     */
    public function usuario(): BelongsTo
    {
        return $this->belongsTo(User::class, 'usuario_id');
    }

    /**
     * Get the calendar sync that owns the reminder.
     */
    public function calendarSync(): BelongsTo
    {
        return $this->belongsTo(CalendarSync::class, 'calendario_sync_id');
    }

    /**
     * Get the reminder information for API responses
     */
    public function toApiArray(): array
    {
        return [
            'id' => $this->id,
            'titulo' => $this->titulo,
            'descripcion' => $this->descripcion,
            'fecha_hora' => $this->fecha_hora->format('Y-m-d H:i:s'),
            'tipo' => $this->tipo,
            'completado' => $this->completado,
            'created_at' => $this->created_at,
            'updated_at' => $this->updated_at,
        ];
    }

    /**
     * Scope a query to only include completed reminders.
     */
    public function scopeCompletado($query)
    {
        return $query->where('completado', true);
    }

    /**
     * Scope a query to only include pending reminders.
     */
    public function scopePendiente($query)
    {
        return $query->where('completado', false);
    }

    /**
     * Scope a query to filter by type.
     */
    public function scopeTipo($query, $tipo)
    {
        return $query->where('tipo', $tipo);
    }

    /**
     * Scope a query to filter reminders for today.
     */
    public function scopeHoy($query)
    {
        return $query->whereDate('fecha_hora', Carbon::today());
    }

    /**
     * Scope a query to filter upcoming reminders.
     */
    public function scopeProximos($query, $days = 7)
    {
        return $query->whereBetween('fecha_hora', [
            Carbon::now(),
            Carbon::now()->addDays($days)
        ]);
    }

    /**
     * Scope a query to filter overdue reminders.
     */
    public function scopeVencidos($query)
    {
        return $query->where('fecha_hora', '<', Carbon::now())
                    ->where('completado', false);
    }

    /**
     * Check if the reminder is overdue.
     */
    public function isOverdue(): bool
    {
        return $this->fecha_hora < Carbon::now() && !$this->completado;
    }

    /**
     * Mark the reminder as completed.
     */
    public function marcarCompletado(): bool
    {
        $this->completado = true;
        return $this->save();
    }

    /**
     * Mark the reminder as pending.
     */
    public function marcarPendiente(): bool
    {
        $this->completado = false;
        return $this->save();
    }
}
