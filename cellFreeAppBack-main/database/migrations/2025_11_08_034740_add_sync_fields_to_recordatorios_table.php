<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddSyncFieldsToRecordatoriosTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('recordatorios', function (Blueprint $table) {
            $table->unsignedBigInteger('calendario_sync_id')->nullable();
            $table->string('uid_externo')->nullable();
            $table->boolean('es_sincronizado')->default(false);
            
            $table->foreign('calendario_sync_id')->references('id')->on('calendar_syncs')->onDelete('cascade');
            $table->index(['usuario_id', 'es_sincronizado']);
            $table->index('uid_externo');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('recordatorios', function (Blueprint $table) {
            $table->dropForeign(['calendario_sync_id']);
            $table->dropColumn(['calendario_sync_id', 'uid_externo', 'es_sincronizado']);
        });
    }
}
