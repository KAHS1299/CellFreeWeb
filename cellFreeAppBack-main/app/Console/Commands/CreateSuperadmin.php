<?php
namespace App\Console\Commands;
use App\Models\User;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Hash;

class CreateSuperadmin extends Command
{
    protected $signature = 'cellfree:superadmin';
    protected $description = 'Crea de forma interactiva el primer superadministrador';
    public function handle(): int
    {
        if (User::where('rol', 'SUPERADMIN')->exists()) { $this->error('Ya existe un superadministrador.'); return 1; }
        $name = $this->ask('Nombre');
        $email = $this->ask('Correo electrónico');
        $password = $this->secret('Contraseña (mínimo 12 caracteres)');
        if (!$name || !filter_var($email, FILTER_VALIDATE_EMAIL) || strlen((string)$password) < 12) { $this->error('Datos inválidos; se requiere correo válido y contraseña de 12 caracteres.'); return 1; }
        if (User::where('correo', $email)->exists()) { $this->error('Ese correo ya está registrado.'); return 1; }
        User::create(['nombre'=>$name,'correo'=>$email,'contraseña'=>Hash::make($password),'rol'=>'SUPERADMIN','activo'=>true]);
        $this->info('Superadministrador creado. Guarda la contraseña de forma segura.');
        return 0;
    }
}
