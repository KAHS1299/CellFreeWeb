<?php

namespace Tests\Feature;

use App\Models\PlanLicencia;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CellFreeAdminIntegrationTest extends TestCase
{
    use RefreshDatabase;

    private function makeUser(string $name, string $email, string $role, ?int $adminId = null): User
    {
        return User::create(['nombre'=>$name,'correo'=>$email,'contraseña'=>Hash::make('SafePassword123!'),'rol'=>$role,'administrador_id'=>$adminId,'activo'=>true]);
    }

    public function test_admin_sees_only_assigned_students(): void
    {
        $admin=$this->makeUser('Admin Uno','admin1@example.test','ADMINISTRADOR');
        $admin->permisos=['users_view','users_manage']; $admin->save();
        $own=$this->makeUser('Estudiante Propio','own@example.test','ESTUDIANTE',$admin->id);
        $otherAdmin=$this->makeUser('Admin Dos','admin2@example.test','ADMINISTRADOR');
        $other=$this->makeUser('Estudiante Ajeno','other@example.test','ESTUDIANTE',$otherAdmin->id);

        $this->actingAs($admin)->get('/admin')->assertForbidden();
        $this->actingAs($admin)->get('/admin/usuarios')->assertOk()->assertSee($own->correo)->assertDontSee($other->correo);
        $this->actingAs($admin)->patch('/admin/usuarios/'.$other->id.'/estado')->assertForbidden();
        $student=$this->makeUser('Estudiante Público','student2@example.test','ESTUDIANTE');
        $this->actingAs($student)->get('/admin/usuarios')->assertForbidden();
    }

    public function test_mobile_can_register_device_sync_apps_and_read_license_features(): void
    {
        $student=$this->makeUser('Estudiante','student@example.test','ESTUDIANTE');
        Sanctum::actingAs($student, ['*']);
        $deviceResponse=$this->postJson('/api/v1/devices',['identificador'=>'device-01','modelo'=>'Pixel','sistema_operativo'=>'Android 14','version_app'=>'1.0'])->assertOk();
        $deviceId=$deviceResponse->json('data.device.id');
        $this->postJson('/api/v1/applications/sync',['dispositivo_id'=>$deviceId,'aplicaciones'=>[['nombre_paquete'=>'com.example.social','nombre'=>'Social','categoria'=>'social']]])->assertOk()->assertJsonPath('data.sincronizadas',1);
        $this->assertDatabaseHas('aplicaciones_instaladas',['dispositivo_id'=>$deviceId,'nombre_paquete'=>'com.example.social']);

        $plan=PlanLicencia::create(['nombre'=>'Premium','descripcion'=>'Plan test','precio'=>0,'duracion_dias'=>30,'funcionalidades'=>['bloqueos','reportes'],'max_usuarios'=>1,'activo'=>true]);
        \App\Models\Licencia::create(['codigo'=>'TEST-CODE','usuario_id'=>$student->id,'plan_id'=>$plan->id,'fecha_activacion'=>now(),'fecha_vencimiento'=>now()->addDays(30),'estado'=>'ACTIVA','metodo_asignacion'=>'MANUAL']);
        $this->getJson('/api/v1/license')->assertOk()->assertJsonPath('data.activa',true)->assertJsonPath('data.funcionalidades.0','bloqueos');
    }

    public function test_expiry_command_updates_license_and_records_history(): void
    {
        $student=$this->makeUser('Vencido','expired@example.test','ESTUDIANTE');
        $plan=PlanLicencia::create(['nombre'=>'Basico','precio'=>0,'duracion_dias'=>30,'funcionalidades'=>[],'max_usuarios'=>1,'activo'=>true]);
        \App\Models\Licencia::create(['codigo'=>'EXPIRED-01','usuario_id'=>$student->id,'plan_id'=>$plan->id,'fecha_activacion'=>now()->subDays(60),'fecha_vencimiento'=>now()->subDay(),'estado'=>'ACTIVA','metodo_asignacion'=>'MANUAL']);

        $this->artisan('cellfree:expire-licenses')->assertExitCode(0);
        $this->assertDatabaseHas('licencias',['codigo'=>'EXPIRED-01','estado'=>'VENCIDA']);
        $this->assertDatabaseHas('historial_licencias',['accion'=>'VENCIDA_AUTOMATICAMENTE']);
    }
}
