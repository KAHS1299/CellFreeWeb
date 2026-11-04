<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMetasGamificadasTable extends Migration
{
    public function up(): void {
        Schema::create('metas_gamificadas', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->string('descripcion', 200);
            $table->integer('puntos')->default(0);
            $table->boolean('completada')->default(false);
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('metas_gamificadas');
    }
}
