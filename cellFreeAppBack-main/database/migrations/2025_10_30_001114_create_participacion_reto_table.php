<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateParticipacionRetoTable extends Migration
{
    public function up(): void {
        Schema::create('participacion_reto', function (Blueprint $table) {
            $table->id();
            $table->foreignId('reto_id')->constrained('retos')->onDelete('cascade');
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->decimal('progreso', 5, 2)->default(0.00);
            $table->boolean('completado')->default(false);
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('participacion_reto');
    }
}
