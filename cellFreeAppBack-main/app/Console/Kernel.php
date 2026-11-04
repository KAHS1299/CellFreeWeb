<?php

namespace App\Console;

use Illuminate\Console\Scheduling\Schedule;
use Illuminate\Foundation\Console\Kernel as ConsoleKernel;

class Kernel extends ConsoleKernel
{
    /**
     * Define the application's command schedule.
     *
     * @param  \Illuminate\Console\Scheduling\Schedule  $schedule
     * @return void
     */
    protected function schedule(Schedule $schedule)
    {
        // Sincroniza los calendarios cada minuto y evita ejecuciones simultáneas
        // si la sincronización anterior todavía está en curso.
        $schedule->command('calendar:sync')
            ->everyMinute()
            ->withoutOverlapping();
        $schedule->command('cellfree:expire-licenses')->dailyAt('00:10')->withoutOverlapping();
    }

    /**
     * Register the commands for the application.
     *
     * @return void
     */
    protected function commands()
    {
        $this->load(__DIR__.'/Commands');

        require base_path('routes/console.php');
    }
}
