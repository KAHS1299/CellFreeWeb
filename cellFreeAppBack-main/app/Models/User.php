<?php

namespace App\Models;

use Illuminate\Contracts\Auth\MustVerifyEmail;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

class User extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    /**
     * The table associated with the model.
     *
     * @var string
     */
    protected $table = 'usuarios';

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'nombre',
        'correo',
        'contraseña',
        'carrera',
        'semestre',
        'apellidos',
        'rol',
        'administrador_id',
        'permisos',
        'activo',
    ];

    /**
     * The attributes that should be hidden for serialization.
     *
     * @var array<int, string>
     */
    protected $hidden = [
        'contraseña',
        'remember_token',
    ];

    /**
     * Get the name of the unique identifier for the user.
     *
     * @return string
     */
    public function getAuthIdentifierName()
    {
        return 'id';
    }

    /**
     * Get the password for the user.
     *
     * @return string
     */
    public function getAuthPassword()
    {
        return $this->contraseña;
    }

    /**
     * Get the column name for the "remember me" token.
     *
     * @return string
     */
    public function getRememberTokenName()
    {
        return 'remember_token';
    }

    /**
     * The attributes that should be cast.
     *
     * @var array<string, string>
     */
    protected $casts = [
        'email_verified_at' => 'datetime',
        'activo' => 'boolean',
        'permisos' => 'array',
    ];

    public function administrador()
    {
        return $this->belongsTo(self::class, 'administrador_id');
    }

    public function estudiantes()
    {
        return $this->hasMany(self::class, 'administrador_id');
    }

    public function licencias()
    {
        return $this->hasMany(\App\Models\Licencia::class, 'usuario_id');
    }

    /**
     * Get the name attribute (maps to nombre).
     */
    public function getNameAttribute()
    {
        return $this->attributes['nombre'];
    }

    /**
     * Set the name attribute (maps to nombre).
     */
    public function setNameAttribute($value)
    {
        $this->attributes['nombre'] = $value;
    }

    /**
     * Get the email attribute (maps to correo).
     */
    public function getEmailAttribute()
    {
        return $this->attributes['correo'];
    }

    /**
     * Set the email attribute (maps to correo).
     */
    public function setEmailAttribute($value)
    {
        $this->attributes['correo'] = $value;
    }

    /**
     * Get the password attribute (maps to contraseña).
     */
    public function getPasswordAttribute()
    {
        return $this->attributes['contraseña'];
    }

    /**
     * Set the password attribute (maps to contraseña).
     */
    public function setPasswordAttribute($value)
    {
        $this->attributes['contraseña'] = $value;
    }

    /**
     * Get the user's full information for API responses
     */
    public function toApiArray(): array
    {
        return [
            'id' => $this->id,
            'name' => $this->nombre,
            'email' => $this->correo,
            'carrera' => $this->carrera,
            'semestre' => $this->semestre,
            'created_at' => $this->created_at,
            'updated_at' => $this->updated_at,
        ];
    }

    /**
     * Create a new personal access token for the user.
     */
    public function createApiToken(string $name = 'auth_token'): string
    {
        return $this->createToken($name)->plainTextToken;
    }
}
