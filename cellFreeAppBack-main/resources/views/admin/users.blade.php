@extends('admin.layout')
@section('title', 'Usuarios')
@section('heading', 'Usuarios y roles')
@section('subheading', 'Gestiona las cuentas con alcance aplicado en el servidor.')

@section('content')
<div class="panel toolbar users-toolbar">
    <form method="get" class="filters">
        <input name="q" value="{{ request('q') }}" placeholder="Buscar nombre o correo" aria-label="Buscar nombre o correo">
        <select name="rol" aria-label="Filtrar por rol">
            <option value="">Todos los roles</option>
            @foreach(['SUPERADMIN', 'ADMINISTRADOR', 'ESTUDIANTE'] as $rol)
                <option value="{{ $rol }}" {{ request('rol') === $rol ? 'selected' : '' }}>{{ $rol }}</option>
            @endforeach
        </select>
        <button class="btn light" type="submit">Filtrar</button>
    </form>
    @if(auth()->user()->rol === 'SUPERADMIN' || in_array('users_manage', (array) auth()->user()->permisos, true))
        <a class="btn primary" href="#nuevo">+ Nuevo usuario</a>
    @endif
</div>

<div class="panel table-wrap users-table-wrap">
    <table class="users-table">
        <thead>
            <tr>
                <th>Usuario</th>
                <th>Correo electrónico</th>
                <th>Rol</th>
                <th>Administrador asignado</th>
                <th>Estado</th>
                <th>Fecha de registro</th>
                <th>Acciones</th>
            </tr>
        </thead>
        <tbody>
            @forelse($users as $u)
                @php
                    $canManageUser = auth()->user()->rol === 'SUPERADMIN'
                        || ((int) $u->administrador_id === auth()->id()
                            && in_array('users_manage', (array) auth()->user()->permisos, true));
                @endphp
                <tr>
                    <td class="user-name"><strong>{{ $u->nombre }} {{ $u->apellidos }}</strong></td>
                    <td class="user-email">{{ $u->correo }}</td>
                    <td><span class="badge">{{ $u->rol }}</span></td>
                    <td>{{ $u->administrador->nombre ?? '—' }}</td>
                    <td><span class="badge {{ $u->activo ? 'good' : 'warn' }}">{{ $u->activo ? 'Activo' : 'Inactivo' }}</span></td>
                    <td>{{ optional($u->created_at)->format('d/m/Y') }}</td>
                    <td>
                        <div class="user-action-group">
                            @if($canManageUser)
                                <button class="btn light" type="button" data-editor-target="user-editor-{{ $u->id }}" aria-controls="user-editor-{{ $u->id }}" aria-expanded="false">Editar usuario</button>
                            @endif
                            @if($canManageUser && $u->rol !== 'SUPERADMIN')
                                <form method="post" action="{{ route('admin.users.toggle', $u) }}" data-confirm="¿Cambiar el estado de esta cuenta?">
                                    @csrf
                                    @method('PATCH')
                                    <button class="link-btn" type="submit">{{ $u->activo ? 'Desactivar' : 'Activar' }}</button>
                                </form>
                                <form method="post" action="{{ route('admin.users.delete', $u) }}" data-confirm="Eliminar solo si no hay datos relacionados; de lo contrario, desactiva la cuenta.">
                                    @csrf
                                    @method('DELETE')
                                    <button class="link-btn danger-link" type="submit">Eliminar</button>
                                </form>
                            @endif
                        </div>
                    </td>
                </tr>
            @empty
                <tr><td colspan="7" class="empty">No hay usuarios que coincidan con la búsqueda.</td></tr>
            @endforelse
        </tbody>
    </table>
    {{ $users->links() }}
</div>

<div class="user-editors" id="user-editors">
    @foreach($users as $u)
        @php
            $canManageUser = auth()->user()->rol === 'SUPERADMIN'
                || ((int) $u->administrador_id === auth()->id()
                    && in_array('users_manage', (array) auth()->user()->permisos, true));
        @endphp
        @if($canManageUser)
            <section class="panel user-editor" id="user-editor-{{ $u->id }}" hidden tabindex="-1">
                <div class="user-editor-heading">
                    <div>
                        <p class="eyebrow">EDITAR CUENTA</p>
                        <h2>{{ $u->nombre }} {{ $u->apellidos }}</h2>
                        <p class="muted">{{ $u->correo }}</p>
                    </div>
                    <button type="button" class="link-btn" data-editor-close>Cerrar</button>
                </div>

                <form method="post" action="{{ route('admin.users.update', $u) }}" class="user-editor-form">
                    @csrf
                    @method('PUT')
                    <div class="form-grid">
                        <label>Nombre<input name="nombre" value="{{ $u->nombre }}" maxlength="100" required></label>
                        <label>Apellidos<input name="apellidos" value="{{ $u->apellidos }}" maxlength="100"></label>
                        <label>Correo electrónico<input type="email" name="correo" value="{{ $u->correo }}" maxlength="100" required></label>
                        @if(auth()->user()->rol === 'SUPERADMIN')
                            <label>Rol
                                <select name="rol">
                                    @foreach(['ESTUDIANTE' => 'ESTUDIANTE', 'ADMINISTRADOR' => 'ADMINISTRADOR', 'SUPERADMIN' => 'SUPERADMIN'] as $roleValue => $roleLabel)
                                        <option value="{{ $roleValue }}" {{ $u->rol === $roleValue ? 'selected' : '' }}>{{ $roleLabel }}</option>
                                    @endforeach
                                </select>
                            </label>
                            <label>Administrador responsable
                                <select name="administrador_id">
                                    <option value="">Sin asignación</option>
                                    @foreach($admins as $a)
                                        <option value="{{ $a->id }}" {{ (int) $u->administrador_id === (int) $a->id ? 'selected' : '' }}>{{ $a->nombre }} · {{ $a->correo }}</option>
                                    @endforeach
                                </select>
                            </label>
                        @else
                            <label>Rol<input value="{{ $u->rol }}" readonly></label>
                            <label>Administrador responsable<input value="{{ $u->administrador->nombre ?? 'Sin asignar' }}" readonly></label>
                        @endif
                        <label>Estado de la cuenta<input value="{{ $u->activo ? 'Activo' : 'Inactivo' }}" readonly></label>
                    </div>
                    <div class="user-form-actions">
                        <button class="btn primary" type="submit">Guardar cambios</button>
                    </div>
                </form>

                @if(auth()->user()->rol === 'SUPERADMIN' && $u->rol === 'ADMINISTRADOR')
                    <form method="post" action="{{ route('admin.admins.permissions', $u) }}" class="user-permissions-form">
                        @csrf
                        @method('PUT')
                        <div class="user-editor-heading permissions-heading">
                            <div>
                                <p class="eyebrow">ACCESO AL PANEL</p>
                                <h3>Permisos del administrador</h3>
                            </div>
                        </div>
                        <div class="permissions-grid">
                            @foreach(['dashboard' => 'Dashboard', 'users_view' => 'Consultar usuarios', 'users_manage' => 'Gestionar usuarios', 'devices_view' => 'Ver dispositivos', 'blocks_manage' => 'Administrar bloqueos', 'licenses_manage' => 'Gestionar licencias', 'reports_view' => 'Ver reportes', 'audit_view' => 'Consultar auditoría'] as $key => $label)
                                <label class="permission-option">
                                    <input type="checkbox" name="permisos[]" value="{{ $key }}" {{ in_array($key, (array) $u->permisos, true) ? 'checked' : '' }}>
                                    <span>{{ $label }}</span>
                                </label>
                            @endforeach
                        </div>
                        <div class="user-form-actions">
                            <button class="btn light" type="submit">Guardar permisos</button>
                        </div>
                    </form>
                @endif
            </section>
        @endif
    @endforeach
</div>

@if(auth()->user()->rol === 'SUPERADMIN' || in_array('users_manage', (array) auth()->user()->permisos, true))
    <section id="nuevo" class="panel form-panel user-create-panel">
        <div class="user-editor-heading">
            <div>
                <p class="eyebrow">NUEVA CUENTA</p>
                <h2>Crear usuario</h2>
            </div>
        </div>
        <form method="post" action="{{ route('admin.users.create') }}" class="form-grid">
            @csrf
            <label>Nombre<input name="nombre" maxlength="100" required></label>
            <label>Apellidos<input name="apellidos" maxlength="100"></label>
            <label>Correo electrónico<input name="correo" type="email" maxlength="100" required></label>
            <label>Contraseña inicial (mínimo 12 caracteres)<input name="password" type="password" minlength="12" autocomplete="new-password" required></label>
            <label>Confirmar contraseña<input name="password_confirmation" type="password" minlength="12" autocomplete="new-password" required></label>
            @if(auth()->user()->rol === 'SUPERADMIN')
                <label>Rol
                    <select name="rol" id="new-user-role">
                        <option value="ESTUDIANTE">ESTUDIANTE</option>
                        <option value="ADMINISTRADOR">ADMINISTRADOR</option>
                        <option value="SUPERADMIN">SUPERADMIN</option>
                    </select>
                </label>
                <label id="new-user-admin-wrapper">Administrador responsable
                    <select name="administrador_id">
                        <option value="">Asignar a quien crea</option>
                        @foreach($admins as $a)
                            <option value="{{ $a->id }}">{{ $a->nombre }} · {{ $a->correo }}</option>
                        @endforeach
                    </select>
                </label>
                <fieldset class="create-permissions" id="new-user-permissions" hidden>
                    <legend>Permisos del administrador</legend>
                    <div class="permissions-grid">
                        @foreach(['dashboard' => 'Dashboard', 'users_view' => 'Consultar usuarios', 'users_manage' => 'Gestionar usuarios', 'devices_view' => 'Ver dispositivos', 'blocks_manage' => 'Administrar bloqueos', 'licenses_manage' => 'Gestionar licencias', 'reports_view' => 'Ver reportes', 'audit_view' => 'Consultar auditoría'] as $key => $label)
                            <label class="permission-option">
                                <input type="checkbox" name="permisos[]" value="{{ $key }}">
                                <span>{{ $label }}</span>
                            </label>
                        @endforeach
                    </div>
                </fieldset>
                <p class="muted create-note" id="new-user-superadmin-note" hidden>SUPERADMIN cuenta con privilegios completos. Los permisos no se asignan individualmente.</p>
            @else
                <input type="hidden" name="rol" value="ESTUDIANTE">
                <p class="muted create-note">La cuenta se creará como estudiante asignado a ti.</p>
            @endif
            <div class="user-form-actions">
                <button class="btn primary" type="submit">Crear cuenta</button>
            </div>
        </form>
    </section>
@endif

<script>
    document.querySelectorAll('[data-editor-target]').forEach(function (button) {
        button.addEventListener('click', function () {
            document.querySelectorAll('.user-editor').forEach(function (panel) { panel.hidden = true; });
            document.querySelectorAll('[data-editor-target]').forEach(function (control) { control.setAttribute('aria-expanded', 'false'); });
            var panel = document.getElementById(button.getAttribute('data-editor-target'));
            if (panel) {
                panel.hidden = false;
                button.setAttribute('aria-expanded', 'true');
                panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
                panel.focus({ preventScroll: true });
            }
        });
    });
    document.querySelectorAll('[data-editor-close]').forEach(function (button) {
        button.addEventListener('click', function () {
            var panel = button.closest('.user-editor');
            if (panel) panel.hidden = true;
        });
    });
    var roleSelect = document.getElementById('new-user-role');
    if (roleSelect) {
        var permissionFieldset = document.getElementById('new-user-permissions');
        var adminWrapper = document.getElementById('new-user-admin-wrapper');
        var superadminNote = document.getElementById('new-user-superadmin-note');
        var updateRoleFields = function () {
            permissionFieldset.hidden = roleSelect.value !== 'ADMINISTRADOR';
            adminWrapper.hidden = roleSelect.value !== 'ESTUDIANTE';
            superadminNote.hidden = roleSelect.value !== 'SUPERADMIN';
            permissionFieldset.querySelectorAll('input[type="checkbox"]').forEach(function (checkbox) {
                checkbox.disabled = roleSelect.value !== 'ADMINISTRADOR';
            });
        };
        roleSelect.addEventListener('change', updateRoleFields);
        updateRoleFields();
    }
</script>
@endsection
