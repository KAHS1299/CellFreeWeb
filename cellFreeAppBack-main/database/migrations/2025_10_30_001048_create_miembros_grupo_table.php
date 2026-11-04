<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMiembrosGrupoTable extends Migration
{
    public function up(): void {
        Schema::create('miembros_grupo', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->foreignId('grupo_id')->constrained('grupos_estudio')->onDelete('cascade');
            $table->enum('rol', ['lider', 'miembro'])->default('miembro');
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('miembros_grupo');
    }
}
