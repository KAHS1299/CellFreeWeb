<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRecordatoriosTable extends Migration
{
    public function up(): void {
        Schema::create('recordatorios', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->string('titulo', 100);
            $table->text('descripcion')->nullable();
            $table->dateTime('fecha_hora');
            $table->enum('tipo', ['tarea', 'examen', 'estudio']);
            $table->boolean('completado')->default(false);
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('recordatorios');
    }
}
