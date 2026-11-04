<?php
namespace App\Models;
use Illuminate\Database\Eloquent\Model;

class PlanLicencia extends Model
{
    protected $table = 'planes_licencia';
    protected $guarded = ['id'];
    protected $casts = ['funcionalidades' => 'array', 'activo' => 'boolean'];
}
