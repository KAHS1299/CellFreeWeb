<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class StudentController extends Controller
{
    public function institution(Request $request): JsonResponse
    {
        $institution = DB::table('licencias')
            ->join('instituciones', 'instituciones.id', '=', 'licencias.institucion_id')
            ->where('licencias.usuario_id', $request->user()->id)
            ->where('licencias.estado', 'ACTIVA')
            ->where(function ($query) {
                $query->whereNull('licencias.fecha_vencimiento')
                    ->orWhere('licencias.fecha_vencimiento', '>=', now());
            })
            ->orderByDesc('licencias.fecha_vencimiento')
            ->select('instituciones.id', 'instituciones.nombre')
            ->first();

        return response()->json([
            'status' => 'success',
            'data' => ['institucion' => $institution],
        ]);
    }

    public function applications(Request $request): JsonResponse
    {
        $applications = DB::table('aplicaciones_instaladas')
            ->join('dispositivos', 'dispositivos.id', '=', 'aplicaciones_instaladas.dispositivo_id')
            ->where('dispositivos.usuario_id', $request->user()->id)
            ->select(
                'aplicaciones_instaladas.id',
                'aplicaciones_instaladas.nombre_paquete',
                'aplicaciones_instaladas.nombre',
                'aplicaciones_instaladas.categoria',
                'aplicaciones_instaladas.sincronizada_en'
            )
            ->orderBy('aplicaciones_instaladas.nombre')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => ['aplicaciones' => $applications],
        ]);
    }

    public function productivity(Request $request): JsonResponse
    {
        $userId = $request->user()->id;
        $today = today()->toDateString();
        $weekStart = now()->startOfWeek()->toDateString();
        $weekEnd = today()->toDateString();
        $seriesStart = now()->subDays(6)->toDateString();

        $dailyFocusTime = DB::table('analisis_uso')
            ->where('usuario_id', $userId)
            ->whereDate('fecha', $today)
            ->sum('tiempo_productivo');

        $weeklyFocusTime = DB::table('analisis_uso')
            ->where('usuario_id', $userId)
            ->whereBetween('fecha', [$weekStart, $weekEnd])
            ->sum('tiempo_productivo');

        $dailyProgress = DB::table('analisis_uso')
            ->where('usuario_id', $userId)
            ->whereBetween('fecha', [$seriesStart, $weekEnd])
            ->select(
                'fecha',
                DB::raw('SUM(tiempo_productivo) as tiempo_productivo'),
                DB::raw('SUM(tiempo_distractor) as tiempo_distractor')
            )
            ->groupBy('fecha')
            ->orderBy('fecha')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => [
                'tiempo_concentracion_hoy' => (int) $dailyFocusTime,
                'tiempo_concentracion_semana' => (int) $weeklyFocusTime,
                'progreso_diario_semanal' => $dailyProgress,
            ],
        ]);
    }

    public function goals(Request $request): JsonResponse
    {
        $goals = DB::table('metas_gamificadas')
            ->where('usuario_id', $request->user()->id)
            ->orderByDesc('created_at')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => ['metas' => $goals],
        ]);
    }

    public function createGoal(Request $request): JsonResponse
    {
        $data = $request->validate([
            'descripcion' => 'required|string|max:200',
        ]);

        $goalId = DB::table('metas_gamificadas')->insertGetId([
            'usuario_id' => $request->user()->id,
            'descripcion' => $data['descripcion'],
            'puntos' => 0,
            'completada' => false,
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        $goal = DB::table('metas_gamificadas')->where('id', $goalId)->first();

        return response()->json([
            'status' => 'success',
            'data' => ['meta' => $goal],
        ], 201);
    }

    public function updateGoal(Request $request, int $id): JsonResponse
    {
        $data = $request->validate([
            'descripcion' => 'required|string|max:200',
        ]);

        $goal = DB::table('metas_gamificadas')
            ->where('id', $id)
            ->where('usuario_id', $request->user()->id)
            ->first();

        abort_unless($goal, 404, 'Meta no encontrada.');

        DB::table('metas_gamificadas')
            ->where('id', $id)
            ->where('usuario_id', $request->user()->id)
            ->update([
                'descripcion' => $data['descripcion'],
                'updated_at' => now(),
            ]);

        $updatedGoal = DB::table('metas_gamificadas')->where('id', $id)->first();

        return response()->json([
            'status' => 'success',
            'data' => ['meta' => $updatedGoal],
        ]);
    }

    public function deleteGoal(Request $request, int $id): JsonResponse
    {
        $deleted = DB::table('metas_gamificadas')
            ->where('id', $id)
            ->where('usuario_id', $request->user()->id)
            ->delete();

        abort_unless($deleted, 404, 'Meta no encontrada.');

        return response()->json([
            'status' => 'success',
            'message' => 'Meta eliminada.',
        ]);
    }

    public function restrictionHistory(Request $request): JsonResponse
    {
        $history = DB::table('aplicaciones_bloqueadas')
            ->where('usuario_id', $request->user()->id)
            ->select(
                'id',
                'nombre_app',
                'categoria',
                'estado',
                'fecha_inicio',
                'fecha_fin',
                'administrador_id',
                'created_at',
                'updated_at'
            )
            ->orderByDesc('updated_at')
            ->get()
            ->map(function ($item) {
                $item->origen = $item->administrador_id ? 'institucional' : 'personal';
                unset($item->administrador_id);

                return $item;
            });

        return response()->json([
            'status' => 'success',
            'data' => ['historial' => $history],
        ]);
    }

    public function rewards(Request $request): JsonResponse
    {
        $userId = $request->user()->id;

        $rewards = DB::table('recompensas')
            ->orderBy('costo_puntos')
            ->get();

        $redemptions = DB::table('canje_recompensas')
            ->join('recompensas', 'recompensas.id', '=', 'canje_recompensas.recompensa_id')
            ->where('canje_recompensas.usuario_id', $userId)
            ->orderByDesc('fecha_canje')
            ->get([
                'canje_recompensas.id',
                'recompensas.nombre',
                'recompensas.descripcion',
                'recompensas.costo_puntos',
                'canje_recompensas.fecha_canje',
            ]);

        $goalPoints = DB::table('metas_gamificadas')
            ->where('usuario_id', $userId)
            ->where('completada', true)
            ->sum('puntos');

        $challengePoints = DB::table('participacion_reto')
            ->join('retos', 'retos.id', '=', 'participacion_reto.reto_id')
            ->where('participacion_reto.usuario_id', $userId)
            ->where('participacion_reto.completado', true)
            ->sum('retos.puntos_recompensa');

        $completedGoals = DB::table('metas_gamificadas')
            ->where('usuario_id', $userId)
            ->where('completada', true)
            ->count();

        $completedChallenges = DB::table('participacion_reto')
            ->where('usuario_id', $userId)
            ->where('completado', true)
            ->count();

        $badges = [];
        if ($completedGoals > 0) {
            $badges[] = [
                'codigo' => 'primera_meta',
                'nombre' => 'Primera meta',
                'progreso' => $completedGoals,
            ];
        }

        if ($completedChallenges > 0) {
            $badges[] = [
                'codigo' => 'primer_reto',
                'nombre' => 'Primer reto',
                'progreso' => $completedChallenges,
            ];
        }

        return response()->json([
            'status' => 'success',
            'data' => [
                'puntos' => (int) $goalPoints + (int) $challengePoints,
                'recompensas' => $rewards,
                'canjes' => $redemptions,
                'insignias' => $badges,
            ],
        ]);
    }

    public function groups(Request $request): JsonResponse
    {
        $groups = DB::table('grupos_estudio')
            ->join('miembros_grupo', 'miembros_grupo.grupo_id', '=', 'grupos_estudio.id')
            ->where('miembros_grupo.usuario_id', $request->user()->id)
            ->select(
                'grupos_estudio.id',
                'grupos_estudio.nombre',
                'grupos_estudio.descripcion',
                'miembros_grupo.rol as mi_rol'
            )
            ->orderBy('grupos_estudio.nombre')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => ['grupos' => $groups],
        ]);
    }

    public function joinGroup(Request $request): JsonResponse
    {
        $data = $request->validate([
            'codigo_invitacion' => 'required|string|max:255',
        ]);

        $membership = DB::transaction(function () use ($request, $data) {
            $group = DB::table('grupos_estudio')
                ->where('codigo_invitacion', $data['codigo_invitacion'])
                ->lockForUpdate()
                ->first();

            abort_unless($group, 404, 'Código de invitación no válido.');

            $alreadyMember = DB::table('miembros_grupo')
                ->where('grupo_id', $group->id)
                ->where('usuario_id', $request->user()->id)
                ->exists();

            if ($alreadyMember) {
                return ['group_id' => $group->id, 'created' => false];
            }

            DB::table('miembros_grupo')->insert([
                'grupo_id' => $group->id,
                'usuario_id' => $request->user()->id,
                'rol' => 'miembro',
                'created_at' => now(),
                'updated_at' => now(),
            ]);

            return ['group_id' => $group->id, 'created' => true];
        });

        return response()->json([
            'status' => 'success',
            'data' => ['grupo_id' => $membership['group_id']],
        ], $membership['created'] ? 201 : 200);
    }

    public function members(Request $request, int $groupId): JsonResponse
    {
        $isMember = DB::table('miembros_grupo')
            ->where('grupo_id', $groupId)
            ->where('usuario_id', $request->user()->id)
            ->exists();

        abort_unless($isMember, 404, 'Grupo no encontrado.');

        $members = DB::table('miembros_grupo')
            ->join('usuarios', 'usuarios.id', '=', 'miembros_grupo.usuario_id')
            ->where('miembros_grupo.grupo_id', $groupId)
            ->select(
                'usuarios.id',
                'usuarios.nombre',
                'usuarios.apellidos',
                'miembros_grupo.rol'
            )
            ->orderBy('usuarios.nombre')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => ['integrantes' => $members],
        ]);
    }

    public function challenges(Request $request): JsonResponse
    {
        $userGroups = DB::table('miembros_grupo')
            ->where('usuario_id', $request->user()->id)
            ->select('grupo_id');

        $challenges = DB::table('retos')
            ->whereIn('grupo_id', $userGroups)
            ->where('fecha_fin', '>=', now())
            ->orderBy('fecha_inicio')
            ->get();

        return response()->json([
            'status' => 'success',
            'data' => ['retos' => $challenges],
        ]);
    }

    public function joinChallenge(Request $request, int $challengeId): JsonResponse
    {
        $created = DB::transaction(function () use ($request, $challengeId) {
            $challenge = DB::table('retos')
                ->where('id', $challengeId)
                ->where('fecha_fin', '>=', now())
                ->lockForUpdate()
                ->first();

            abort_unless($challenge, 404, 'Reto no disponible.');

            $isMember = DB::table('miembros_grupo')
                ->where('grupo_id', $challenge->grupo_id)
                ->where('usuario_id', $request->user()->id)
                ->exists();

            abort_unless($isMember, 404, 'Reto no disponible.');

            $alreadyParticipating = DB::table('participacion_reto')
                ->where('reto_id', $challengeId)
                ->where('usuario_id', $request->user()->id)
                ->exists();

            if ($alreadyParticipating) {
                return false;
            }

            DB::table('participacion_reto')->insert([
                'reto_id' => $challengeId,
                'usuario_id' => $request->user()->id,
                'progreso' => 0,
                'completado' => false,
                'created_at' => now(),
                'updated_at' => now(),
            ]);

            return true;
        });

        return response()->json([
            'status' => 'success',
            'message' => $created ? 'Participación registrada.' : 'Ya participas en este reto.',
        ], $created ? 201 : 200);
    }
}
