<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRetosTable extends Migration
{
    public function up(): void {
        Schema::create('retos', function (Blueprint $table) {
            $table->id();
            $table->foreignId('grupo_id')->constrained('grupos_estudio')->onDelete('cascade');
            $table->string('nombre', 100);
            $table->text('descripcion')->nullable();
            $table->integer('puntos_recompensa')->default(0);
            $table->dateTime('fecha_inicio');
            $table->dateTime('fecha_fin');
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('retos');
    }
}
