<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddFechaInicioFinToAplicacionesBloqueadasTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('aplicaciones_bloqueadas', function (Blueprint $table) {
            $table->datetime('fecha_inicio')->nullable()->after('estado')
                  ->comment('Fecha y hora de inicio del bloqueo');
            $table->datetime('fecha_fin')->nullable()->after('fecha_inicio')
                  ->comment('Fecha y hora de fin del bloqueo');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('aplicaciones_bloqueadas', function (Blueprint $table) {
            $table->dropColumn(['fecha_inicio', 'fecha_fin']);
        });
    }
}
