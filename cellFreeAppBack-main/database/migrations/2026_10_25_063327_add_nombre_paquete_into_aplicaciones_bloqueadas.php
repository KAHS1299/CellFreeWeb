<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddNombrePaqueteIntoAplicacionesBloqueadas extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('aplicaciones_bloqueadas', function (Blueprint $table) {
            $table->string('nombre_paquete', 100)->nullable()->
                  comment('Nombre del paquete bloqueado');
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
            $table->dropColumn('nombre_paquete');
        });
    }
}
