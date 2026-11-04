<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Hash;

class ResetSuperadminPassword extends Command
{
    protected $signature = 'cellfree:superadmin:reset-password';

    protected $description = 'Restablece interactivamente la contraseña de un superadministrador existente';

    public function handle(): int
    {
        $email = mb_strtolower(trim((string) $this->ask('Correo del superadministrador')));

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $this->error('El correo no es válido.');
            return 1;
        }

        $user = User::where('correo', $email)
            ->where('rol', 'SUPERADMIN')
            ->first();

        if (!$user) {
            $this->error('No se encontró un superadministrador con ese correo. No se modificó ningún registro.');
            return 1;
        }

        $password = $this->secret('Nueva contraseña (mínimo 12 caracteres)');
        $confirmation = $this->secret('Confirma la nueva contraseña');

        if (strlen((string) $password) < 12) {
            $this->error('La contraseña debe tener al menos 12 caracteres. No se modificó ningún registro.');
            return 1;
        }

        if (!hash_equals((string) $password, (string) $confirmation)) {
            $this->error('Las contraseñas no coinciden. No se modificó ningún registro.');
            return 1;
        }

        $user->contraseña = Hash::make($password);
        $user->save();

        $this->info('Contraseña actualizada. El correo, rol y estado del superadministrador no cambiaron.');

        return 0;
    }
}
