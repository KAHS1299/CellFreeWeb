<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCalendarSyncsTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('calendar_syncs', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('usuario_id');
            $table->string('nombre', 100);
            $table->text('url_calendario');
            $table->enum('estado', ['activo', 'inactivo', 'error'])->default('activo');
            $table->integer('intervalo_sincronizacion')->default(60); // minutos
            $table->timestamp('ultima_sincronizacion')->nullable();
            $table->text('ultimo_error')->nullable();
            $table->json('configuracion')->nullable(); // Configuraciones adicionales
            $table->timestamps();

            $table->foreign('usuario_id')->references('id')->on('usuarios')->onDelete('cascade');
            $table->index(['usuario_id', 'estado']);
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('calendar_syncs');
    }
}
