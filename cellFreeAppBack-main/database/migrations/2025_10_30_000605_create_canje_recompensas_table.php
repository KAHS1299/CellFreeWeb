<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCanjeRecompensasTable extends Migration
{
    public function up(): void {
        Schema::create('canje_recompensas', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->foreignId('recompensa_id')->constrained('recompensas')->onDelete('cascade');
            $table->timestamp('fecha_canje')->useCurrent();
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('canje_recompensas');
    }
}
