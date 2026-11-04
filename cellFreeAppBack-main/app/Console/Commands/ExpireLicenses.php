<?php
namespace App\Console\Commands;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

class ExpireLicenses extends Command
{
    protected $signature='cellfree:expire-licenses';
    protected $description='Marca como vencidas las licencias activas cuya fecha ya pasó';
    public function handle(): int
    {
        $total=0;
        DB::table('licencias')->where('estado','ACTIVA')->whereNotNull('fecha_vencimiento')->where('fecha_vencimiento','<=',now())->orderBy('id')->chunkById(200,function($licenses)use(&$total){
            foreach($licenses as $license){
                $changed=DB::table('licencias')->where('id',$license->id)->where('estado','ACTIVA')->update(['estado'=>'VENCIDA','updated_at'=>now()]);
                if(!$changed)continue;
                DB::table('historial_licencias')->insert(['licencia_id'=>$license->id,'actor_id'=>null,'accion'=>'VENCIDA_AUTOMATICAMENTE','detalle'=>json_encode(['fecha_vencimiento'=>$license->fecha_vencimiento]),'created_at'=>now(),'updated_at'=>now()]);
                DB::table('registro_actividad')->insert(['actor_id'=>null,'accion'=>'licencia.vencida_automaticamente','entidad'=>'licencias','entidad_id'=>$license->id,'detalle'=>null,'ip'=>null,'created_at'=>now(),'updated_at'=>now()]);
                $total++;
            }
        });
        $this->info("Licencias marcadas como vencidas: {$total}");
        return 0;
    }
}
