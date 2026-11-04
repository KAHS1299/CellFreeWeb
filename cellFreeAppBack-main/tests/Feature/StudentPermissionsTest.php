<?php

namespace Tests\Feature;

use App\Models\Licencia;
use App\Models\PlanLicencia;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class StudentPermissionsTest extends TestCase
{
    use RefreshDatabase;

    private function student(string $email = 'student@example.test'): User
    {
        return User::create([
            'nombre' => 'Estudiante',
            'apellidos' => 'Prueba',
            'correo' => $email,
            'contraseña' => Hash::make('SafePassword123!'),
            'rol' => 'ESTUDIANTE',
            'activo' => true,
        ]);
    }

    public function test_student_can_read_own_profile_but_cannot_use_admin_panel_or_other_user_identity(): void
    {
        $student = $this->student();
        $other = $this->student('other@example.test');
        Sanctum::actingAs($student, ['*']);

        $this->getJson('/api/auth/user?user_id='.$other->id)
            ->assertOk()
            ->assertJsonPath('data.user.id', $student->id)
            ->assertJsonPath('data.user.rol', 'ESTUDIANTE');
        $this->getJson('/api/auth/user/'.$other->id)->assertNotFound();
        $this->putJson('/api/auth/update-profile', [
            'name' => 'Nombre actualizado',
            'email' => 'updated@example.test',
            'password' => 'NewSafePassword123!',
            'password_confirmation' => 'NewSafePassword123!',
            'rol' => 'SUPERADMIN',
            'permisos' => ['users_manage'],
            'activo' => false,
            'user_id' => $other->id,
        ])->assertOk();
        $this->assertDatabaseHas('usuarios', [
            'id' => $student->id,
            'nombre' => 'Nombre actualizado',
            'correo' => 'updated@example.test',
            'rol' => 'ESTUDIANTE',
            'activo' => true,
        ]);
        $this->assertTrue(Hash::check('NewSafePassword123!', $student->fresh()->getAuthPassword()));
        $this->assertDatabaseMissing('usuarios', ['id' => $other->id, 'nombre' => 'Nombre actualizado']);
        $this->get('/admin/usuarios')->assertForbidden();
    }

    public function test_administrators_and_inactive_accounts_cannot_use_student_mobile_endpoints(): void
    {
        $admin = User::create([
            'nombre' => 'Admin',
            'correo' => 'admin@example.test',
            'contraseña' => Hash::make('SafePassword123!'),
            'rol' => 'ADMINISTRADOR',
            'activo' => true,
        ]);
        Sanctum::actingAs($admin, ['*']);
        $this->getJson('/api/auth/student/productivity')->assertForbidden();

        $student = $this->student();
        $student->activo = false;
        $student->save();
        Sanctum::actingAs($student, ['*']);
        $this->getJson('/api/auth/student/productivity')->assertForbidden();
    }

    public function test_student_cannot_change_institutional_blocks_and_can_manage_only_owned_reminders_and_goals(): void
    {
        $student = $this->student();
        $other = $this->student('other@example.test');
        $blockId = DB::table('aplicaciones_bloqueadas')->insertGetId(['usuario_id' => $student->id, 'administrador_id' => $other->id, 'nombre_app' => 'Bloqueo escolar', 'estado' => true, 'created_at' => now(), 'updated_at' => now()]);
        Sanctum::actingAs($student, ['*']);

        $this->putJson('/api/app-blocks/'.$blockId, ['estado' => false])->assertNotFound();
        $this->deleteJson('/api/app-blocks/'.$blockId)->assertNotFound();
        $this->assertDatabaseHas('aplicaciones_bloqueadas', ['id' => $blockId, 'estado' => true]);
        $this->getJson('/api/auth/student/restrictions/history')->assertOk()->assertJsonPath('data.historial.0.origen', 'institucional');

        $reminder = $this->postJson('/api/reminders', ['titulo' => 'Estudiar', 'descripcion' => 'Propio', 'fecha_hora' => now()->addDay()->toDateTimeString(), 'tipo' => 'estudio', 'user_id' => $other->id])->assertCreated();
        $reminderId = $reminder->json('data.recordatorio.id');
        $this->assertDatabaseHas('recordatorios', ['id' => $reminderId, 'usuario_id' => $student->id]);
        $this->putJson('/api/reminders/'.$reminderId, ['titulo' => 'Editado', 'user_id' => $other->id])->assertOk();
        $this->deleteJson('/api/reminders/'.$reminderId)->assertOk();
        $this->assertDatabaseMissing('recordatorios', ['id' => $reminderId]);

        $goal = $this->postJson('/api/auth/student/goals', ['descripcion' => 'Meta propia', 'user_id' => $other->id, 'puntos' => 900, 'completada' => true])->assertCreated();
        $goalId = $goal->json('data.meta.id');
        $this->assertDatabaseHas('metas_gamificadas', ['id' => $goalId, 'usuario_id' => $student->id, 'puntos' => 0, 'completada' => false]);
        $this->putJson('/api/auth/student/goals/'.$goalId, ['descripcion' => 'Meta actualizada', 'user_id' => $other->id])->assertOk();
        $this->assertDatabaseHas('metas_gamificadas', ['id' => $goalId, 'usuario_id' => $student->id, 'descripcion' => 'Meta actualizada']);
        $this->deleteJson('/api/auth/student/goals/'.$goalId)->assertOk();
    }

    public function test_student_can_read_own_productivity_rewards_groups_challenges_and_license(): void
    {
        $student = $this->student();
        $other = $this->student('other@example.test');
        Sanctum::actingAs($student, ['*']);
        DB::table('analisis_uso')->insert(['usuario_id' => $student->id, 'fecha' => today(), 'tiempo_productivo' => 45, 'tiempo_distractor' => 10, 'created_at' => now(), 'updated_at' => now()]);
        DB::table('analisis_uso')->insert(['usuario_id' => $other->id, 'fecha' => today(), 'tiempo_productivo' => 999, 'tiempo_distractor' => 0, 'created_at' => now(), 'updated_at' => now()]);
        $this->getJson('/api/auth/student/productivity')->assertOk()->assertJsonPath('data.tiempo_concentracion_hoy', 45);

        $plan = PlanLicencia::create(['nombre' => 'Plan estudiante', 'precio' => 0, 'duracion_dias' => 30, 'funcionalidades' => [], 'max_usuarios' => 1, 'activo' => true]);
        DB::table('instituciones')->insert(['id' => 1, 'nombre' => 'Universidad demo', 'activo' => true, 'created_at' => now(), 'updated_at' => now()]);
        Licencia::create(['codigo' => 'STUDENT-TEST', 'usuario_id' => $student->id, 'plan_id' => $plan->id, 'institucion_id' => 1, 'fecha_activacion' => now(), 'fecha_vencimiento' => now()->addDays(30), 'estado' => 'ACTIVA', 'metodo_asignacion' => 'MANUAL']);
        $this->getJson('/api/v1/license')->assertOk()->assertJsonPath('data.activa', true);
        $this->getJson('/api/auth/student/institution')->assertOk()->assertJsonPath('data.institucion.nombre', 'Universidad demo');
        $this->getJson('/api/auth/student/rewards')->assertOk()->assertJsonPath('data.puntos', 0)->assertJsonStructure(['data' => ['recompensas', 'insignias', 'canjes']]);

        $groupId = DB::table('grupos_estudio')->insertGetId(['nombre' => 'Grupo A', 'descripcion' => 'Prueba', 'codigo_invitacion' => 'JOIN-A', 'created_at' => now(), 'updated_at' => now()]);
        DB::table('miembros_grupo')->insert(['usuario_id' => $student->id, 'grupo_id' => $groupId, 'rol' => 'miembro', 'created_at' => now(), 'updated_at' => now()]);
        DB::table('miembros_grupo')->insert(['usuario_id' => $other->id, 'grupo_id' => $groupId, 'rol' => 'miembro', 'created_at' => now(), 'updated_at' => now()]);
        $challengeId = DB::table('retos')->insertGetId(['grupo_id' => $groupId, 'nombre' => 'Reto A', 'puntos_recompensa' => 10, 'fecha_inicio' => now()->subDay(), 'fecha_fin' => now()->addDay(), 'created_at' => now(), 'updated_at' => now()]);
        $this->getJson('/api/auth/student/groups')->assertOk()->assertJsonFragment(['nombre' => 'Grupo A']);
        $this->getJson('/api/auth/student/groups/'.$groupId.'/members')->assertOk()->assertJsonFragment(['nombre' => 'Estudiante']);
        $this->getJson('/api/auth/student/challenges')->assertOk()->assertJsonFragment(['nombre' => 'Reto A']);
        $this->postJson('/api/auth/student/challenges/'.$challengeId.'/join', ['user_id' => $other->id])->assertCreated();
        $this->assertDatabaseHas('participacion_reto', ['reto_id' => $challengeId, 'usuario_id' => $student->id, 'progreso' => 0]);
        $this->postJson('/api/auth/student/groups/join', ['codigo_invitacion' => 'JOIN-A', 'user_id' => $other->id])->assertOk();
        $this->assertDatabaseHas('miembros_grupo', ['grupo_id' => $groupId, 'usuario_id' => $student->id]);
    }
}
