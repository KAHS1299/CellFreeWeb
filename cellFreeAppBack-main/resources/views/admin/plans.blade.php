@extends('admin.layout')
@section('title','Planes y licencias')
@section('heading','Planes y licencias')
@section('subheading','Configuración comercial y asignación de accesos. La plataforma no procesa pagos.')
@section('content')
@if(auth()->user()->rol==='SUPERADMIN')
<div class="columns">
    <section class="panel form-panel">
        <h2>Crear plan</h2>
        <form method="post" action="{{ route('admin.plans.create') }}" class="form-grid single">@csrf
            <label>Nombre<input name="nombre" required placeholder="Premium"></label>
            <label>Descripción<textarea name="descripcion"></textarea></label>
            <div class="form-grid"><label>Precio<input name="precio" type="number" min="0" step="0.01" required value="0"></label><label>Duración (días)<input name="duracion_dias" type="number" min="1" value="30" required></label></div>
            <label>Máximo de usuarios<input name="max_usuarios" type="number" min="1" value="1" required></label>
            <label>Funcionalidades (separadas por coma)<input name="funcionalidades" placeholder="bloqueos, reportes"></label>
            <button class="btn primary">Guardar plan</button>
        </form>
    </section>
    <section class="panel">
        <h2>Planes configurados</h2>
        @forelse($plans as $p)
        <details class="row"><summary class="grow"><b>{{ $p->nombre }}</b><small>{{ $p->duracion_dias }} días · {{ $p->activo?'Activo':'Inactivo' }}</small></summary><form method="post" action="{{ route('admin.plans.update',$p) }}" class="form-grid single">@csrf @method('PUT')<label>Nombre<input name="nombre" value="{{ $p->nombre }}" required></label><label>Descripción<textarea name="descripcion">{{ $p->descripcion }}</textarea></label><div class="form-grid"><label>Precio<input name="precio" type="number" min="0" step="0.01" value="{{ $p->precio }}" required></label><label>Duración (días)<input name="duracion_dias" type="number" min="1" value="{{ $p->duracion_dias }}" required></label></div><label>Máximo de usuarios<input name="max_usuarios" type="number" min="1" value="{{ $p->max_usuarios }}" required></label><label>Funcionalidades<input name="funcionalidades" value="{{ implode(', ',(array)$p->funcionalidades) }}"></label><label><input style="width:auto" type="checkbox" name="activo" value="1" @checked($p->activo)> Plan activo</label><button class="btn light">Guardar cambios</button></form></details>
        @empty<div class="empty">No hay planes configurados.</div>@endforelse
    </section>
</div>
@endif
<section class="panel form-panel"><h2>Asignar licencia</h2><form method="post" action="{{ route('admin.licenses.create') }}" class="form-grid">@csrf<label>Usuario<select name="usuario_id" required>@foreach(\App\Models\User::where('rol','ESTUDIANTE')->when(auth()->user()->rol!=='SUPERADMIN',fn($q)=>$q->where('administrador_id',auth()->id()))->orderBy('nombre')->get() as $u)<option value="{{ $u->id }}">{{ $u->nombre }} · {{ $u->correo }}</option>@endforeach</select></label><label>Plan<select name="plan_id" required>@foreach($plans->where('activo',true) as $p)<option value="{{ $p->id }}">{{ $p->nombre }} — {{ $p->duracion_dias }} días</option>@endforeach</select></label><label>Institución (opcional)<select name="institucion_id"><option value="">Sin institución</option>@foreach($institutions as $institution)<option value="{{ $institution->id }}">{{ $institution->nombre }}</option>@endforeach</select></label><label>Fecha de activación<input type="datetime-local" name="fecha_activacion" value="{{ now()->format('Y-m-d\TH:i') }}" required></label><button class="btn primary">Asignar licencia</button></form></section>
<section class="panel table-wrap"><h2>Licencias registradas</h2><table><thead><tr><th>Código</th><th>Usuario</th><th>Plan</th><th>Activación</th><th>Vencimiento</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>@forelse($licenses as $l)<tr><td>{{ $l->codigo }}</td><td>{{ $l->usuario->nombre ?? 'Sin asignar' }}</td><td>{{ $l->plan->nombre }}</td><td>{{ optional($l->fecha_activacion)->format('d/m/Y') }}</td><td>{{ optional($l->fecha_vencimiento)->format('d/m/Y') }}</td><td><span class="badge {{ $l->estado==='ACTIVA' && (!$l->fecha_vencimiento || $l->fecha_vencimiento->isFuture())?'good':'warn' }}">{{ $l->estado==='ACTIVA' && $l->fecha_vencimiento && $l->fecha_vencimiento->isPast()?'VENCIDA':$l->estado }}</span></td><td><details><summary class="link-btn">Gestionar</summary><form method="post" action="{{ route('admin.licenses.action',$l) }}" class="form-grid single" data-confirm="¿Aplicar este cambio de licencia?">@csrf @method('PATCH')<label>Acción<select name="accion"><option value="RENOVAR">Renovar</option><option value="CAMBIAR_PLAN">Cambiar plan</option><option value="SUSPENDER">Suspender</option><option value="REACTIVAR">Reactivar</option>@if(auth()->user()->rol==='SUPERADMIN')<option value="CANCELAR">Cancelar</option>@endif</select></label><label>Plan nuevo<select name="plan_id"><option value="">Seleccionar cuando se cambia el plan</option>@foreach($plans->where('activo',true) as $p)<option value="{{ $p->id }}">{{ $p->nombre }}</option>@endforeach</select></label><button class="btn light">Aplicar cambio</button></form></details></td></tr>@empty<tr><td colspan="7" class="empty">Aún no hay licencias.</td></tr>@endforelse</tbody></table>{{ $licenses->links() }}</section>
@endsection
