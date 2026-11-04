<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateGruposEstudioTable extends Migration
{
    public function up(): void {
        Schema::create('grupos_estudio', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 100);
            $table->text('descripcion')->nullable();
            $table->string('codigo_invitacion', 10)->unique();
            $table->timestamps();
        });
    }

    public function down(): void {
        Schema::dropIfExists('grupos_estudio');
    }
}
