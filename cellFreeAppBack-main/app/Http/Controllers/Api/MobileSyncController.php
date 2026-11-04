<?php
namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Licencia;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class MobileSyncController extends Controller
{
    public function registerDevice(Request $request)
    {
        $data = $request->validate(['identificador'=>'required|string|max:160','modelo'=>'nullable|string|max:160','sistema_operativo'=>'nullable|string|max:60','version_app'=>'nullable|string|max:40']);
        $device = DB::table('dispositivos')->where('usuario_id',$request->user()->id)->where('identificador',$data['identificador'])->first();
        $payload = $data + ['usuario_id'=>$request->user()->id,'ultima_sincronizacion'=>now(),'updated_at'=>now()];
        if ($device) DB::table('dispositivos')->where('id',$device->id)->update($payload);
        else { $payload['created_at']=now(); $id=DB::table('dispositivos')->insertGetId($payload); $device=DB::table('dispositivos')->find($id); }
        return response()->json(['status'=>'success','data'=>['device'=>$device]]);
    }

    public function syncApps(Request $request)
    {
        $data = $request->validate(['dispositivo_id'=>'required|integer','aplicaciones'=>'required|array|max:1000','aplicaciones.*.nombre_paquete'=>'required|string|max:190','aplicaciones.*.nombre'=>'nullable|string|max:160','aplicaciones.*.categoria'=>'nullable|string|max:80']);
        $device = DB::table('dispositivos')->where('id',$data['dispositivo_id'])->where('usuario_id',$request->user()->id)->first();
        abort_unless($device, 404, 'Dispositivo no encontrado.');
        DB::transaction(function () use ($data, $device) {
            foreach ($data['aplicaciones'] as $app) DB::table('aplicaciones_instaladas')->updateOrInsert(['dispositivo_id'=>$device->id,'nombre_paquete'=>$app['nombre_paquete']], ['nombre'=>$app['nombre']??null,'categoria'=>$app['categoria']??null,'sincronizada_en'=>now(),'updated_at'=>now(),'created_at'=>now()]);
            DB::table('dispositivos')->where('id',$device->id)->update(['ultima_sincronizacion'=>now(),'updated_at'=>now()]);
        });
        return response()->json(['status'=>'success','data'=>['sincronizadas'=>count($data['aplicaciones'])]]);
    }

    public function license(Request $request)
    {
        $license = Licencia::with('plan')->where('usuario_id',$request->user()->id)->where('estado','ACTIVA')->orderByDesc('fecha_vencimiento')->first();
        if (!$license || ($license->fecha_vencimiento && $license->fecha_vencimiento->isPast())) return response()->json(['status'=>'success','data'=>['activa'=>false,'funcionalidades'=>[],'plan'=>null]]);
        return response()->json(['status'=>'success','data'=>['activa'=>true,'codigo'=>$license->codigo,'estado'=>$license->estado,'vencimiento'=>$license->fecha_vencimiento,'plan'=>$license->plan->nombre,'funcionalidades'=>$license->plan->funcionalidades ?? []]]);
    }
}
