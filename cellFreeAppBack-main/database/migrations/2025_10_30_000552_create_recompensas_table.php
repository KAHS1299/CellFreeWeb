<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRecompensasTable extends Migration
{
    public function up(): void {
        Schema::create('recompensas', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 100);
            $table->text('descripcion')->nullable();
            $table->integer('costo_puntos');
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('recompensas');
    }
}
