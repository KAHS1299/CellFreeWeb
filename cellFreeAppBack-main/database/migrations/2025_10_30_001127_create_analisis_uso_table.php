<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateAnalisisUsoTable extends Migration
{
    public function up(): void {
        Schema::create('analisis_uso', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->onDelete('cascade');
            $table->date('fecha');
            $table->integer('tiempo_productivo')->default(0);
            $table->integer('tiempo_distractor')->default(0);
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('analisis_uso');
    }
}
