<?php
namespace App\Models;
use Illuminate\Database\Eloquent\Model;

class Licencia extends Model
{
    protected $table = 'licencias';
    protected $guarded = ['id'];
    protected $casts = ['fecha_activacion' => 'datetime', 'fecha_vencimiento' => 'datetime'];
    public function plan() { return $this->belongsTo(PlanLicencia::class, 'plan_id'); }
    public function usuario() { return $this->belongsTo(User::class, 'usuario_id'); }
}
