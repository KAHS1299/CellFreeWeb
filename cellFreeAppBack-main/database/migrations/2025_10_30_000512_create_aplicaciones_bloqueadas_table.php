<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAplicacionesBloqueadasTable extends Migration
{
    public function up(): void {
        Schema::create('aplicaciones_bloqueadas', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->string('nombre_app', 100);
            $table->string('categoria', 50)->nullable();
            $table->integer('tiempo_bloqueo')->nullable();
            $table->boolean('estado')->default(true);
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('aplicaciones_bloqueadas');
    }
}
