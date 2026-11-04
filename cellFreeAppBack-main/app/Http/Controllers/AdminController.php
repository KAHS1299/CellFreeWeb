<?php

namespace App\Http\Controllers;

use App\Models\Licencia;
use App\Models\PlanLicencia;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

class AdminController extends Controller
{
    public function login(Request $request)
    {
        $data = $request->validate(['email' => 'required|email', 'password' => 'required|string']);
        if (!Auth::attempt(['correo' => $data['email'], 'password' => $data['password'], 'activo' => true])) {
            return back()->withErrors(['email' => 'Credenciales incorrectas o cuenta inactiva.'])->withInput($request->only('email'));
        }
        $request->session()->regenerate();
        if (!in_array(Auth::user()->rol, ['SUPERADMIN', 'ADMINISTRADOR'], true)) {
            Auth::logout();
            $request->session()->invalidate();
            $request->session()->regenerateToken();
            return back()->withErrors(['email' => 'El panel está disponible solo para administradores.']);
        }
        return redirect()->route('admin.dashboard');
    }

    public function dashboard()
    {
        $this->requirePermission('dashboard');
        $q = $this->visibleUsers();
        $userIds = (clone $q)->pluck('id');
        return view('admin.dashboard', [
            'counts' => [
                'usuarios' => (clone $q)->count(),
                'administradores' => User::where('rol', 'ADMINISTRADOR')->when(Auth::user()->rol !== 'SUPERADMIN', fn($q) => $q->where('id', Auth::id()))->count(),
                'estudiantes' => (clone $q)->where('rol', 'ESTUDIANTE')->count(),
                'bloqueos' => \App\Models\AplicacionBloqueada::whereIn('usuario_id', $userIds)->where('estado', true)->count(),
                'licencias' => Licencia::whereIn('usuario_id', $userIds)->where('estado', 'ACTIVA')->where(function($q){$q->whereNull('fecha_vencimiento')->orWhere('fecha_vencimiento','>',now());})->count(),
                'licencias_vencidas' => Licencia::whereIn('usuario_id',$userIds)->where(function($q){$q->where('estado','VENCIDA')->orWhere(function($sub){$sub->where('estado','ACTIVA')->whereNotNull('fecha_vencimiento')->where('fecha_vencimiento','<=',now());});})->count(),
                'dispositivos' => \Illuminate\Support\Facades\DB::table('dispositivos')->whereIn('usuario_id', $userIds)->count(),
            ],
            'recentUsers' => (clone $q)->latest()->take(7)->get(),
            'expiring' => Licencia::with(['usuario', 'plan'])->whereIn('usuario_id', $userIds)->where('estado', 'ACTIVA')->whereBetween('fecha_vencimiento', [now(), now()->addDays(30)])->orderBy('fecha_vencimiento')->take(6)->get(),
        ]);
    }

    public function users(Request $request)
    {
        abort_unless(Auth::user()->rol === 'SUPERADMIN' || in_array('users_view', (array) Auth::user()->permisos, true) || in_array('users_manage', (array) Auth::user()->permisos, true), 403);
        $filters = $request->validate([
            'q' => 'nullable|string|max:120',
            'rol' => 'nullable|in:SUPERADMIN,ADMINISTRADOR,ESTUDIANTE',
        ]);
        $users = $this->visibleUsers()->with('administrador:id,nombre,correo')->when(!empty($filters['q']), function ($q) use ($filters) {
            $term = '%'.$filters['q'].'%';
            $q->where(function ($sub) use ($term) { $sub->where('nombre', 'like', $term)->orWhere('apellidos', 'like', $term)->orWhere('correo', 'like', $term); });
        })->when(!empty($filters['rol']), fn($q) => $q->where('rol', $filters['rol']))->orderByDesc('id')->paginate(15)->withQueryString();
        $admins = Auth::user()->rol === 'SUPERADMIN'
            ? User::whereIn('rol', ['ADMINISTRADOR', 'SUPERADMIN'])->where('activo', true)->orderBy('nombre')->get(['id', 'nombre', 'correo', 'rol'])
            : collect();
        return view('admin.users', compact('users', 'admins'));
    }

    public function blocks(Request $request)
    {
        $this->requirePermission('blocks_manage');
        $userIds = $this->visibleUsers()->pluck('id');
        $blocks = \App\Models\AplicacionBloqueada::with('usuario')->whereIn('usuario_id', $userIds)->when($request->filled('q'), function ($q) use ($request) { $term='%'.$request->q.'%'; $q->where(function ($filter) use ($term) { $filter->where('nombre_app','like',$term)->orWhere('nombre_paquete','like',$term); }); })->latest()->paginate(20)->withQueryString();
        $students = $this->visibleUsers()->where('rol','ESTUDIANTE')->orderBy('nombre')->get();
        return view('admin.blocks', compact('blocks','students'));
    }

    public function createBlock(Request $request)
    {
        $this->requirePermission('blocks_manage');
        $data=$request->validate(['usuario_id'=>'required|exists:usuarios,id','dispositivo_id'=>'nullable|integer','nombre_app'=>'required|string|max:100','nombre_paquete'=>'required|string|max:190','fecha_inicio'=>'nullable|date','fecha_fin'=>'nullable|date|after:fecha_inicio']);
        abort_unless($this->visibleUsers()->where('id',$data['usuario_id'])->exists(),403);
        if (!empty($data['dispositivo_id'])) abort_unless(\Illuminate\Support\Facades\DB::table('dispositivos')->where('id',$data['dispositivo_id'])->where('usuario_id',$data['usuario_id'])->exists(),403);
        $block=\App\Models\AplicacionBloqueada::create(['usuario_id'=>$data['usuario_id'],'dispositivo_id'=>$data['dispositivo_id']??null,'nombre_app'=>$data['nombre_app'],'nombre_paquete'=>$data['nombre_paquete'],'fecha_inicio'=>$data['fecha_inicio']??null,'fecha_fin'=>$data['fecha_fin']??null,'estado'=>true,'administrador_id'=>Auth::id()]);
        $this->audit('bloqueo.creado','aplicaciones_bloqueadas',$block->id);
        return back()->with('success','Bloqueo guardado para la sincronización de la app móvil.');
    }

    public function importBlocks(Request $request)
    {
        $this->requirePermission('blocks_manage');
        $request->validate(['archivo'=>'required|file|mimes:csv,txt|max:2048']);
        $handle=fopen($request->file('archivo')->getRealPath(),'r');
        $header=fgetcsv($handle,0,';');
        $expected=['correo','dispositivo','nombre_paquete','nombre_app','fecha_inicio','fecha_fin'];
        if (!$header || array_map('trim',$header)!==$expected) { fclose($handle); return back()->withErrors(['archivo'=>'Encabezados requeridos, en orden: '.implode(';',$expected)]); }
        $valid=[]; $rejected=[]; $line=1; $visible=$this->visibleUsers()->pluck('id')->all();
        while (($row=fgetcsv($handle,0,';'))!==false) {
            $line++; if (count($row)!==6) { $rejected[]="Fila {$line}: se esperaban 6 columnas."; continue; }
            [$email,$deviceId,$package,$appName,$start,$end]=array_map('trim',$row);
            $user=User::where('correo',$email)->whereIn('id',$visible)->first();
            $device=$user ? \Illuminate\Support\Facades\DB::table('dispositivos')->where('usuario_id',$user->id)->where('identificador',$deviceId)->first() : null;
            try { $from=$start ? \Carbon\Carbon::parse($start) : null; $to=$end ? \Carbon\Carbon::parse($end) : null; } catch (\Throwable $e) { $from=null; $to=null; }
            if (!$user || !$device || !$package || !$appName || !$from || !$to || $to->lte($from)) { $rejected[]="Fila {$line}: usuario/dispositivo no autorizado, campos vacíos o rango de fechas inválido."; continue; }
            $valid[]=['usuario_id'=>$user->id,'dispositivo_id'=>$device->id,'nombre_paquete'=>$package,'nombre_app'=>$appName,'fecha_inicio'=>$from->toDateTimeString(),'fecha_fin'=>$to->toDateTimeString()];
        }
        fclose($handle);
        session(['csv_block_rows'=>$valid,'csv_block_rejected'=>$rejected]);
        return back()->with('csv_preview',['valid'=>$valid,'rejected'=>$rejected]);
    }

    public function confirmImportBlocks()
    {
        $this->requirePermission('blocks_manage');
        $rows=session('csv_block_rows',[]); $visible=$this->visibleUsers()->pluck('id')->all(); $ok=0; $rejected=session('csv_block_rejected',[]);
        foreach ($rows as $index=>$row) {
            if (!in_array($row['usuario_id'],$visible,true)) { $rejected[]='Registro '.($index+1).': el usuario ya no está dentro del alcance.'; continue; }
            \App\Models\AplicacionBloqueada::updateOrCreate(['usuario_id'=>$row['usuario_id'],'nombre_paquete'=>$row['nombre_paquete']],$row+['administrador_id'=>Auth::id(),'estado'=>true]); $ok++;
        }
        session()->forget(['csv_block_rows','csv_block_rejected']);
        $this->audit('bloqueos.importados','aplicaciones_bloqueadas',null,['exitosos'=>$ok,'rechazados'=>count($rejected)]);
        return back()->with('import_summary',['ok'=>$ok,'rejected'=>$rejected]);
    }

    public function toggleBlock(\App\Models\AplicacionBloqueada $block)
    {
        $this->requirePermission('blocks_manage');
        abort_unless($this->visibleUsers()->where('id',$block->usuario_id)->exists(),403);
        $block->estado=!$block->estado; $block->save();
        $this->audit('bloqueo.estado','aplicaciones_bloqueadas',$block->id,['estado'=>$block->estado]);
        return back()->with('success','Estado del bloqueo actualizado.');
    }

    public function createUser(Request $request)
    {
        $isSuperadmin = Auth::user()->rol === 'SUPERADMIN';
        abort_unless($isSuperadmin || in_array('users_manage', (array) Auth::user()->permisos, true), 403);
        $request->merge(['correo' => mb_strtolower(trim((string) $request->input('correo')))]);
        $roleRule = $isSuperadmin ? 'required|in:SUPERADMIN,ADMINISTRADOR,ESTUDIANTE' : 'required|in:ESTUDIANTE';
        $data = $request->validate(['nombre'=>'required|string|max:100','apellidos'=>'nullable|string|max:100','correo'=>'required|email|max:100|unique:usuarios,correo','password'=>'required|string|min:12|confirmed','rol'=>$roleRule,'administrador_id'=>'nullable|integer|exists:usuarios,id','permisos'=>'nullable|array','permisos.*'=>'string|in:dashboard,users_view,users_manage,devices_view,blocks_manage,licenses_manage,reports_view,audit_view']);
        $data['contraseña'] = Hash::make($data['password']); unset($data['password']);
        unset($data['password_confirmation']);
        if ($data['rol'] === 'ESTUDIANTE') {
            $data['administrador_id'] = $isSuperadmin ? ($data['administrador_id'] ?? Auth::id()) : Auth::id();
            abort_unless(User::whereIn('rol', ['ADMINISTRADOR', 'SUPERADMIN'])->where('activo', true)->where('id', $data['administrador_id'])->exists(), 422, 'El responsable seleccionado no es un administrador activo.');
            $data['permisos'] = null;
        } else {
            $data['administrador_id'] = null;
            if ($data['rol'] !== 'ADMINISTRADOR') $data['permisos'] = null;
            else $data['permisos'] = array_values(array_unique($data['permisos'] ?? []));
        }
        if ($data['rol'] === 'SUPERADMIN') $data['permisos'] = null;
        $user = DB::transaction(function () use ($data) {
            $created = User::create($data + ['activo' => true]);
            $this->audit('usuario.creado', 'usuarios', $created->id, ['rol'=>$created->rol]);
            return $created;
        });
        return back()->with('success', 'Usuario creado.');
    }

    public function toggleUser(User $user)
    {
        $this->requirePermission('users_manage');
        abort_unless(Auth::user()->rol === 'SUPERADMIN' || (int)$user->administrador_id === Auth::id(), 403);
        abort_if($user->id === Auth::id(), 422, 'No puedes desactivar tu propia cuenta.');
        abort_if($user->rol === 'SUPERADMIN', 422, 'El estado de las cuentas SUPERADMIN no se cambia desde este módulo.');
        abort_if($user->rol === 'ADMINISTRADOR' && $user->activo && User::where('administrador_id', $user->id)->exists(), 422, 'Reasigna primero los usuarios a cargo antes de desactivar este administrador.');
        DB::transaction(function () use ($user) {
            $lockedUser = User::whereKey($user->id)->lockForUpdate()->firstOrFail();
            abort_unless(Auth::user()->rol === 'SUPERADMIN' || (int) $lockedUser->administrador_id === Auth::id(), 403);
            abort_if($lockedUser->id === Auth::id() || $lockedUser->rol === 'SUPERADMIN', 422, 'No se puede cambiar el estado de esta cuenta.');
            abort_if($lockedUser->rol === 'ADMINISTRADOR' && $lockedUser->activo && User::where('administrador_id', $lockedUser->id)->exists(), 422, 'Reasigna primero los usuarios a cargo antes de desactivar este administrador.');
            $lockedUser->activo = !$lockedUser->activo;
            $lockedUser->save();
            $this->audit('usuario.estado', 'usuarios', $lockedUser->id, ['activo'=>$lockedUser->activo]);
        });
        return back()->with('success', 'Estado actualizado.');
    }

    public function updateUser(Request $request, User $user)
    {
        $this->requirePermission('users_manage');
        abort_unless(Auth::user()->rol === 'SUPERADMIN' || (int)$user->administrador_id === Auth::id(), 403);
        $request->merge(['correo' => mb_strtolower(trim((string) $request->input('correo')))]);
        $data=$request->validate(['nombre'=>'required|string|max:100','apellidos'=>'nullable|string|max:100','correo'=>'required|email|max:100|unique:usuarios,correo,'.$user->id,'rol'=>'sometimes|required|in:SUPERADMIN,ADMINISTRADOR,ESTUDIANTE','administrador_id'=>'nullable|exists:usuarios,id']);
        if (isset($data['rol']) || array_key_exists('administrador_id',$data)) {
            abort_unless(Auth::user()->rol==='SUPERADMIN',403);
            $newRole=$data['rol']??$user->rol;
            abort_if($user->id===Auth::id() && $newRole!==$user->rol,422,'No puedes cambiar tu propio rol.');
            abort_if($user->rol==='SUPERADMIN' && $newRole!=='SUPERADMIN' && $user->activo && User::where('rol','SUPERADMIN')->where('activo',true)->count()<=1,422,'Debe permanecer al menos un superadministrador activo.');
            abort_if($newRole === 'ESTUDIANTE' && User::where('administrador_id', $user->id)->exists(), 422, 'Reasigna primero los usuarios a cargo antes de cambiar el rol de este administrador.');
            $data['administrador_id']=$newRole==='ESTUDIANTE' ? ($data['administrador_id']??$user->administrador_id??Auth::id()) : null;
            if ($newRole==='ESTUDIANTE') abort_unless(User::whereIn('rol',['ADMINISTRADOR','SUPERADMIN'])->where('activo',true)->where('id',$data['administrador_id'])->exists(),422,'El responsable seleccionado no es un administrador activo.');
            if ($newRole!=='ADMINISTRADOR') $data['permisos']=null;
        }
        DB::transaction(function () use ($user, $data) {
            $user->update($data);
            $this->audit('usuario.actualizado', 'usuarios', $user->id);
        });
        return back()->with('success','Información de usuario actualizada.');
    }

    public function deleteUser(User $user)
    {
        $this->requirePermission('users_manage');
        abort_unless(Auth::user()->rol === 'SUPERADMIN' || (int)$user->administrador_id === Auth::id(), 403);
        abort_if($user->id === Auth::id() || $user->rol === 'SUPERADMIN', 422, 'No se puede eliminar esta cuenta.');
        DB::transaction(function () use ($user) {
            $lockedUser = User::whereKey($user->id)->lockForUpdate()->firstOrFail();
            abort_unless(Auth::user()->rol === 'SUPERADMIN' || (int) $lockedUser->administrador_id === Auth::id(), 403);
            abort_if($lockedUser->id === Auth::id() || $lockedUser->rol === 'SUPERADMIN', 422, 'No se puede eliminar esta cuenta.');
            $dependencies = $this->userDeletionDependencies($lockedUser);
            abort_if($dependencies, 422, 'No se puede eliminar esta cuenta porque tiene datos relacionados: '.implode(', ', $dependencies).'. Desactívala para conservar su historial.');
            $this->audit('usuario.eliminado', 'usuarios', $lockedUser->id, ['correo' => $lockedUser->correo]);
            $lockedUser->delete();
        });
        return back()->with('success','Usuario eliminado.');
    }

    public function plans()
    {
        $this->requirePermission('licenses_manage');
        $userIds=$this->visibleUsers()->pluck('id');
        $plans=PlanLicencia::when(Auth::user()->rol!=='SUPERADMIN',fn($q)=>$q->where('activo',true))->orderBy('nombre')->get();
        $institutions=\Illuminate\Support\Facades\DB::table('instituciones')->where('activo',true)->orderBy('nombre')->get();
        return view('admin.plans', ['plans'=>$plans, 'institutions'=>$institutions, 'licenses'=>Licencia::with(['plan','usuario'])->whereIn('usuario_id',$userIds)->latest()->paginate(15)]);
    }

    public function devices(Request $request)
    {
        $this->requirePermission('devices_view');
        $ids=$this->visibleUsers()->pluck('id');
        $devices=\Illuminate\Support\Facades\DB::table('dispositivos')->join('usuarios','usuarios.id','=','dispositivos.usuario_id')->whereIn('dispositivos.usuario_id',$ids)->when($request->filled('q'),function($q)use($request){$term='%'.$request->q.'%';$q->where(function($s)use($term){$s->where('usuarios.nombre','like',$term)->orWhere('dispositivos.identificador','like',$term)->orWhere('dispositivos.modelo','like',$term);});})->select('dispositivos.*','usuarios.nombre as propietario')->orderByDesc('ultima_sincronizacion')->paginate(20)->withQueryString();
        $apps=\Illuminate\Support\Facades\DB::table('aplicaciones_instaladas')->join('dispositivos','dispositivos.id','=','aplicaciones_instaladas.dispositivo_id')->join('usuarios','usuarios.id','=','dispositivos.usuario_id')->whereIn('dispositivos.usuario_id',$ids)->when($request->filled('app'),function($q)use($request){$term='%'.$request->app.'%';$q->where(function($s)use($term){$s->where('aplicaciones_instaladas.nombre','like',$term)->orWhere('aplicaciones_instaladas.nombre_paquete','like',$term);});})->select('aplicaciones_instaladas.*','dispositivos.identificador','usuarios.nombre as propietario')->orderByDesc('sincronizada_en')->paginate(20,['*'],'apps_page')->withQueryString();
        return view('admin.devices',compact('devices','apps'));
    }

    public function createPlan(Request $request)
    {
        abort_unless(Auth::user()->rol === 'SUPERADMIN', 403);
        $data = $request->validate(['nombre'=>'required|string|max:80|unique:planes_licencia,nombre','descripcion'=>'nullable|string|max:2000','precio'=>'required|numeric|min:0','duracion_dias'=>'required|integer|min:1','max_usuarios'=>'required|integer|min:1','funcionalidades'=>'nullable|string']);
        $features = array_values(array_filter(array_map('trim', explode(',', $data['funcionalidades'] ?? '')))); unset($data['funcionalidades']);
        PlanLicencia::create($data + ['funcionalidades'=>$features,'activo'=>true]);
        $this->audit('plan.creado', 'planes_licencia', null, ['nombre'=>$data['nombre']]);
        return back()->with('success', 'Plan creado.');
    }

    public function updatePlan(Request $request, PlanLicencia $plan)
    {
        abort_unless(Auth::user()->rol==='SUPERADMIN',403);
        $data=$request->validate(['nombre'=>'required|string|max:80|unique:planes_licencia,nombre,'.$plan->id,'descripcion'=>'nullable|string|max:2000','precio'=>'required|numeric|min:0','duracion_dias'=>'required|integer|min:1','max_usuarios'=>'required|integer|min:1','funcionalidades'=>'nullable|string','activo'=>'nullable|boolean']);
        $data['funcionalidades']=array_values(array_filter(array_map('trim',explode(',',$data['funcionalidades']??''))));
        $data['activo']=$request->boolean('activo');
        $plan->update($data); $this->audit('plan.actualizado','planes_licencia',$plan->id);
        return back()->with('success','Plan actualizado.');
    }

    public function licenseAction(Request $request, Licencia $license)
    {
        $this->requirePermission('licenses_manage');
        abort_unless($license->usuario_id && $this->visibleUsers()->where('id',$license->usuario_id)->exists(),403);
        $data=$request->validate(['accion'=>'required|in:RENOVAR,CAMBIAR_PLAN,SUSPENDER,REACTIVAR,CANCELAR','plan_id'=>'nullable|exists:planes_licencia,id']);
        $before=['estado'=>$license->estado,'plan_id'=>$license->plan_id,'fecha_vencimiento'=>$license->fecha_vencimiento];
        switch ($data['accion']) {
            case 'RENOVAR': abort_if($license->estado==='CANCELADA',422,'Una licencia cancelada no se puede renovar.'); $license->fecha_vencimiento=\Carbon\Carbon::parse($license->fecha_vencimiento && $license->fecha_vencimiento->isFuture() ? $license->fecha_vencimiento : now())->addDays($license->plan->duracion_dias); $license->fecha_activacion=$license->fecha_activacion??now(); $license->estado='ACTIVA'; break;
            case 'CAMBIAR_PLAN': abort_unless(Auth::user()->rol==='SUPERADMIN' || !empty($data['plan_id']),403); $newPlan=PlanLicencia::where('activo',true)->findOrFail($data['plan_id']??0); $license->plan_id=$newPlan->id; break;
            case 'SUSPENDER': $license->estado='SUSPENDIDA'; break;
            case 'REACTIVAR': abort_unless(in_array($license->estado,['SUSPENDIDA','PENDIENTE'],true),422,'Solo se pueden reactivar licencias suspendidas o pendientes.'); abort_if($license->fecha_vencimiento && $license->fecha_vencimiento->isPast(),422,'Renueva la licencia vencida antes de reactivarla.'); $license->estado='ACTIVA'; break;
            case 'CANCELAR': abort_unless(Auth::user()->rol==='SUPERADMIN',403); $license->estado='CANCELADA'; break;
        }
        $license->save();
        \Illuminate\Support\Facades\DB::table('historial_licencias')->insert(['licencia_id'=>$license->id,'actor_id'=>Auth::id(),'accion'=>$data['accion'],'detalle'=>json_encode(['antes'=>$before,'despues'=>['estado'=>$license->estado,'plan_id'=>$license->plan_id,'fecha_vencimiento'=>$license->fecha_vencimiento]]),'created_at'=>now(),'updated_at'=>now()]);
        $this->audit('licencia.'.$data['accion'],'licencias',$license->id);
        return back()->with('success','Licencia actualizada; no se procesó ningún pago.');
    }

    public function institutions()
    {
        abort_unless(Auth::user()->rol==='SUPERADMIN',403);
        $institutions=\Illuminate\Support\Facades\DB::table('instituciones')->orderBy('nombre')->get();
        return view('admin.institutions',compact('institutions'));
    }

    public function createInstitution(Request $request)
    {
        abort_unless(Auth::user()->rol==='SUPERADMIN',403);
        $data=$request->validate(['nombre'=>'required|string|max:160','correo_contacto'=>'nullable|email|max:190']);
        $id=\Illuminate\Support\Facades\DB::table('instituciones')->insertGetId($data+['activo'=>true,'created_at'=>now(),'updated_at'=>now()]);
        $this->audit('institucion.creada','instituciones',$id);
        return back()->with('success','Institución creada.');
    }

    public function updateInstitution(Request $request, int $id)
    {
        abort_unless(Auth::user()->rol==='SUPERADMIN',403);
        $data=$request->validate(['nombre'=>'required|string|max:160','correo_contacto'=>'nullable|email|max:190','activo'=>'required|boolean']);
        \Illuminate\Support\Facades\DB::table('instituciones')->where('id',$id)->update($data+['updated_at'=>now()]);
        $this->audit('institucion.actualizada','instituciones',$id);
        return back()->with('success','Institución actualizada.');
    }

    public function createLicense(Request $request)
    {
        $this->requirePermission('licenses_manage');
        $data = $request->validate(['usuario_id'=>'required|exists:usuarios,id','plan_id'=>'required|exists:planes_licencia,id','fecha_activacion'=>'required|date','institucion_id'=>'nullable|exists:instituciones,id']);
        abort_unless($this->visibleUsers()->where('id',$data['usuario_id'])->where('rol','ESTUDIANTE')->exists(),403);
        $plan = PlanLicencia::findOrFail($data['plan_id']);
        abort_unless($plan->activo,422,'El plan está inactivo.');
        $start=\Carbon\Carbon::parse($data['fecha_activacion']);
        $license = Licencia::create(['codigo'=>strtoupper(Str::random(4).'-'.Str::random(4).'-'.Str::random(4)),'usuario_id'=>$data['usuario_id'],'plan_id'=>$plan->id,'institucion_id'=>$data['institucion_id'] ?? null,'creada_por'=>Auth::id(),'fecha_activacion'=>$start,'fecha_vencimiento'=>$start->copy()->addDays($plan->duracion_dias),'estado'=>$start->isFuture()?'PENDIENTE':'ACTIVA','metodo_asignacion'=>'MANUAL']);
        \Illuminate\Support\Facades\DB::table('historial_licencias')->insert(['licencia_id'=>$license->id,'actor_id'=>Auth::id(),'accion'=>'CREADA','detalle'=>json_encode(['plan'=>$plan->nombre]),'created_at'=>now(),'updated_at'=>now()]);
        $this->audit('licencia.creada', 'licencias', $license->id, ['codigo'=>$license->codigo]);
        return back()->with('success', 'Licencia asignada. No se registró ningún pago.');
    }

    public function reports()
    {
        $this->requirePermission('reports_view');
        $ids=$this->visibleUsers()->pluck('id');
        $distribution=\Illuminate\Support\Facades\DB::table('licencias')->join('planes_licencia','planes_licencia.id','=','licencias.plan_id')->whereIn('licencias.usuario_id',$ids)->groupBy('planes_licencia.nombre')->select('planes_licencia.nombre',\Illuminate\Support\Facades\DB::raw('count(*) as total'))->orderByDesc('total')->get();
        return view('admin.reports',['users'=>$ids->count(),'blocks'=>\App\Models\AplicacionBloqueada::whereIn('usuario_id',$ids)->count(),'activeBlocks'=>\App\Models\AplicacionBloqueada::whereIn('usuario_id',$ids)->where('estado',true)->count(),'licenses'=>Licencia::whereIn('usuario_id',$ids)->count(),'activeLicenses'=>Licencia::whereIn('usuario_id',$ids)->where('estado','ACTIVA')->where(function($q){$q->whereNull('fecha_vencimiento')->orWhere('fecha_vencimiento','>',now());})->count(),'expiredLicenses'=>Licencia::whereIn('usuario_id',$ids)->where(function($q){$q->where('estado','VENCIDA')->orWhere(function($sub){$sub->where('estado','ACTIVA')->whereNotNull('fecha_vencimiento')->where('fecha_vencimiento','<=',now());});})->count(),'devices'=>\Illuminate\Support\Facades\DB::table('dispositivos')->whereIn('usuario_id',$ids)->count(),'apps'=>\Illuminate\Support\Facades\DB::table('aplicaciones_instaladas')->join('dispositivos','dispositivos.id','=','aplicaciones_instaladas.dispositivo_id')->whereIn('dispositivos.usuario_id',$ids)->count(),'distribution'=>$distribution]);
    }

    public function exportReport(Request $request)
    {
        $this->requirePermission('reports_view');
        $type=$request->query('tipo'); abort_unless(in_array($type,['usuarios','bloqueos','licencias'],true),404);
        $ids=$this->visibleUsers()->pluck('id');
        return response()->streamDownload(function()use($type,$ids){
            $out=fopen('php://output','w'); fputs($out,"\xEF\xBB\xBF");
            if($type==='usuarios'){fputcsv($out,['id','nombre','apellidos','correo','rol','estado','administrador_id','registro'],';'); User::whereIn('id',$ids)->orderBy('id')->chunk(500,function($rows)use($out){foreach($rows as $u)fputcsv($out,[$u->id,$u->nombre,$u->apellidos,$u->correo,$u->rol,$u->activo?'activo':'inactivo',$u->administrador_id,$u->created_at],';');});}
            if($type==='bloqueos'){fputcsv($out,['id','usuario_id','aplicacion','paquete','inicio','fin','estado'],';'); \App\Models\AplicacionBloqueada::whereIn('usuario_id',$ids)->orderBy('id')->chunk(500,function($rows)use($out){foreach($rows as $b)fputcsv($out,[$b->id,$b->usuario_id,$b->nombre_app,$b->nombre_paquete,$b->fecha_inicio,$b->fecha_fin,$b->estado?'activo':'inactivo'],';');});}
            if($type==='licencias'){fputcsv($out,['codigo','usuario_id','plan_id','activacion','vencimiento','estado'],';'); Licencia::whereIn('usuario_id',$ids)->orderBy('id')->chunk(500,function($rows)use($out){foreach($rows as $l)fputcsv($out,[$l->codigo,$l->usuario_id,$l->plan_id,$l->fecha_activacion,$l->fecha_vencimiento,$l->estado],';');});}
            fclose($out);
        },$type.'_cellfree_'.now()->format('Ymd_His').'.csv',['Content-Type'=>'text/csv; charset=UTF-8']);
    }

    public function auditLog()
    {
        $this->requirePermission('audit_view');
        $logs=\Illuminate\Support\Facades\DB::table('registro_actividad')->leftJoin('usuarios','usuarios.id','=','registro_actividad.actor_id')->when(Auth::user()->rol!=='SUPERADMIN',fn($q)=>$q->where('registro_actividad.actor_id',Auth::id()))->select('registro_actividad.*','usuarios.nombre as actor')->orderByDesc('registro_actividad.id')->paginate(30);
        return view('admin.audit',compact('logs'));
    }

    private function visibleUsers()
    {
        return User::query()->when(Auth::user()->rol !== 'SUPERADMIN', function ($q) { $q->where(function ($sub) { $sub->where('id', Auth::id())->orWhere('administrador_id', Auth::id()); }); });
    }

    public function updateAdminPermissions(Request $request, User $user)
    {
        abort_unless(Auth::user()->rol === 'SUPERADMIN' && $user->rol === 'ADMINISTRADOR',403);
        $allowed=['dashboard','users_view','users_manage','devices_view','blocks_manage','licenses_manage','reports_view','audit_view'];
        $validated=$request->validate(['permisos'=>'sometimes|array','permisos.*'=>'required|string|in:'.implode(',', $allowed)]);
        $permissions=$validated['permisos'] ?? [];
        DB::transaction(function () use ($user, $permissions) {
            $user->permisos = array_values(array_unique($permissions));
            $user->save();
            $this->audit('admin.permisos', 'usuarios', $user->id, ['permisos'=>$user->permisos]);
        });
        return back()->with('success','Permisos del administrador actualizados.');
    }

    private function requirePermission(string $permission): void
    {
        abort_unless(Auth::user()->rol === 'SUPERADMIN' || in_array($permission,(array)Auth::user()->permisos,true),403);
    }

    private function userDeletionDependencies(User $user): array
    {
        $references = [
            ['usuarios', 'administrador_id', 'usuarios asignados'],
            ['aplicaciones_bloqueadas', 'usuario_id', 'bloqueos'],
            ['aplicaciones_bloqueadas', 'administrador_id', 'bloqueos creados'],
            ['recordatorios', 'usuario_id', 'recordatorios'],
            ['metas_gamificadas', 'usuario_id', 'metas'],
            ['canje_recompensas', 'usuario_id', 'canjes de recompensas'],
            ['miembros_grupo', 'usuario_id', 'membresías de grupos'],
            ['participacion_reto', 'usuario_id', 'participaciones en retos'],
            ['analisis_uso', 'usuario_id', 'análisis de uso'],
            ['calendar_syncs', 'usuario_id', 'sincronizaciones de calendario'],
            ['dispositivos', 'usuario_id', 'dispositivos y aplicaciones sincronizadas'],
            ['licencias', 'usuario_id', 'licencias asignadas'],
            ['licencias', 'creada_por', 'licencias creadas'],
            ['historial_licencias', 'actor_id', 'historial de licencias'],
            ['registro_actividad', 'actor_id', 'registros de auditoría'],
        ];

        $dependencies = [];
        foreach ($references as [$table, $column, $label]) {
            if (Schema::hasTable($table) && DB::table($table)->where($column, $user->id)->exists()) {
                $dependencies[] = $label;
            }
        }

        if (Schema::hasTable('registro_actividad') && DB::table('registro_actividad')
            ->where('entidad', 'usuarios')->where('entidad_id', $user->id)->exists()) {
            $dependencies[] = 'historial de cambios de cuenta';
        }

        if (Schema::hasTable('personal_access_tokens') && DB::table('personal_access_tokens')
            ->where('tokenable_type', User::class)->where('tokenable_id', $user->id)->exists()) {
            $dependencies[] = 'tokens de acceso';
        }

        if (Schema::hasTable('password_resets') && DB::table('password_resets')->where('email', $user->correo)->exists()) {
            $dependencies[] = 'restablecimientos de contraseña pendientes';
        }

        return $dependencies;
    }

    private function audit(string $action, ?string $entity, ?int $id, array $details = []): void
    {
        \Illuminate\Support\Facades\DB::table('registro_actividad')->insert(['actor_id'=>Auth::id(),'accion'=>$action,'entidad'=>$entity,'entidad_id'=>$id,'detalle'=>json_encode($details),'ip'=>request()->ip(),'created_at'=>now(),'updated_at'=>now()]);
    }
}
