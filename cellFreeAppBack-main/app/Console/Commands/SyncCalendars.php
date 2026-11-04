<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use App\Services\CalendarSyncService;

class SyncCalendars extends Command
{
    /**
     * The name and signature of the console command.
     *
     * @var string
     */
    protected $signature = 'calendar:sync {--user_id= : Sincronizar solo un usuario específico}';

    /**
     * The console command description.
     *
     * @var string
     */
    protected $description = 'Sincronizar todos los calendarios externos configurados';

    public function handle()
    {
        $this->info('Iniciando sincronización de calendarios...');

        $syncService = new CalendarSyncService();
        
        try {
            $resultados = $syncService->sincronizarTodas();
            
            $total = count($resultados);
            $exitosas = array_filter($resultados, fn($r) => $r['resultado']['success']);
            $fallidas = array_filter($resultados, fn($r) => !$r['resultado']['success']);

            $this->info("Sincronización completada:");
            $this->line("  - Total: {$total}");
            $this->line("  - Exitosas: " . count($exitosas));
            $this->line("  - Fallidas: " . count($fallidas));

            // Mostrar detalles de las sincronizaciones exitosas
            foreach ($exitosas as $resultado) {
                $this->info("✓ {$resultado['nombre']}: {$resultado['resultado']['eventos_importados']} eventos importados");
            }

            // Mostrar errores
            foreach ($fallidas as $resultado) {
                $this->error("✗ {$resultado['nombre']}: {$resultado['resultado']['error']}");
            }

            return Command::SUCCESS;

        } catch (\Exception $e) {
            $this->error('Error durante la sincronización: ' . $e->getMessage());
            return Command::FAILURE;
        }
    }
}
