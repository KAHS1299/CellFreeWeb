<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateCellfreeAdminModules extends Migration
{
    public function up(): void
    {
        Schema::table('usuarios', function (Blueprint $table) {
            $table->string('rol', 24)->default('ESTUDIANTE')->index();
            $table->unsignedBigInteger('administrador_id')->nullable()->index();
            $table->json('permisos')->nullable();
            $table->foreign('administrador_id')->references('id')->on('usuarios')->nullOnDelete();
            $table->boolean('activo')->default(true)->index();
            $table->string('apellidos', 100)->nullable();
        });

        Schema::create('instituciones', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 160);
            $table->string('correo_contacto')->nullable();
            $table->boolean('activo')->default(true);
            $table->timestamps();
        });

        Schema::create('planes_licencia', function (Blueprint $table) {
            $table->id();
            $table->string('nombre', 80)->unique();
            $table->text('descripcion')->nullable();
            $table->decimal('precio', 10, 2)->default(0);
            $table->unsignedInteger('duracion_dias')->default(30);
            $table->json('funcionalidades')->nullable();
            $table->unsignedInteger('max_usuarios')->default(1);
            $table->boolean('activo')->default(true);
            $table->timestamps();
        });

        Schema::create('licencias', function (Blueprint $table) {
            $table->id();
            $table->string('codigo', 40)->unique();
            $table->foreignId('usuario_id')->nullable()->constrained('usuarios')->nullOnDelete();
            $table->foreignId('plan_id')->constrained('planes_licencia')->restrictOnDelete();
            $table->foreignId('institucion_id')->nullable()->constrained('instituciones')->nullOnDelete();
            $table->foreignId('creada_por')->nullable()->constrained('usuarios')->nullOnDelete();
            $table->dateTime('fecha_activacion')->nullable();
            $table->dateTime('fecha_vencimiento')->nullable()->index();
            $table->string('estado', 20)->default('PENDIENTE')->index();
            $table->string('metodo_asignacion', 30)->default('MANUAL');
            $table->timestamps();
        });

        Schema::create('historial_licencias', function (Blueprint $table) {
            $table->id();
            $table->foreignId('licencia_id')->constrained('licencias')->cascadeOnDelete();
            $table->foreignId('actor_id')->nullable()->constrained('usuarios')->nullOnDelete();
            $table->string('accion', 40);
            $table->json('detalle')->nullable();
            $table->timestamps();
        });

        Schema::create('dispositivos', function (Blueprint $table) {
            $table->id();
            $table->foreignId('usuario_id')->constrained('usuarios')->cascadeOnDelete();
            $table->string('identificador', 160);
            $table->string('modelo')->nullable();
            $table->string('sistema_operativo', 60)->nullable();
            $table->string('version_app', 40)->nullable();
            $table->timestamp('ultima_sincronizacion')->nullable();
            $table->timestamps();
            $table->unique(['usuario_id', 'identificador']);
        });

        Schema::create('aplicaciones_instaladas', function (Blueprint $table) {
            $table->id();
            $table->foreignId('dispositivo_id')->constrained('dispositivos')->cascadeOnDelete();
            $table->string('nombre_paquete', 190);
            $table->string('nombre', 160)->nullable();
            $table->string('categoria', 80)->nullable();
            $table->timestamp('sincronizada_en')->nullable();
            $table->timestamps();
            $table->unique(['dispositivo_id', 'nombre_paquete']);
        });

        Schema::table('aplicaciones_bloqueadas', function (Blueprint $table) {
            $table->foreignId('dispositivo_id')->nullable()->after('usuario_id')->constrained('dispositivos')->nullOnDelete();
            $table->foreignId('administrador_id')->nullable()->after('dispositivo_id')->constrained('usuarios')->nullOnDelete();
        });

        Schema::create('registro_actividad', function (Blueprint $table) {
            $table->id();
            $table->foreignId('actor_id')->nullable()->constrained('usuarios')->nullOnDelete();
            $table->string('accion', 120);
            $table->string('entidad', 80)->nullable();
            $table->unsignedBigInteger('entidad_id')->nullable();
            $table->json('detalle')->nullable();
            $table->string('ip', 45)->nullable();
            $table->timestamps();
            $table->index(['entidad', 'entidad_id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('registro_actividad');
        Schema::table('aplicaciones_bloqueadas', function (Blueprint $table) {
            $table->dropConstrainedForeignId('administrador_id');
            $table->dropConstrainedForeignId('dispositivo_id');
        });
        Schema::dropIfExists('aplicaciones_instaladas');
        Schema::dropIfExists('dispositivos');
        Schema::dropIfExists('historial_licencias');
        Schema::dropIfExists('licencias');
        Schema::dropIfExists('planes_licencia');
        Schema::dropIfExists('instituciones');
        Schema::table('usuarios', function (Blueprint $table) {
            $table->dropForeign(['administrador_id']);
            $table->dropColumn(['rol', 'administrador_id', 'permisos', 'activo', 'apellidos']);
        });
    }
}
