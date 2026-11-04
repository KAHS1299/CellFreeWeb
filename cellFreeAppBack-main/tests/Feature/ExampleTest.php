<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ExampleTest extends TestCase
{
    /**
     * A basic test example.
     *
     * @return void
     */
    public function test_root_redirects_to_admin_login()
    {
        $response = $this->get('/');

        $response->assertRedirect('/admin/login');
    }

    public function test_admin_login_page_is_available()
    {
        $this->get('/admin/login')->assertOk()->assertSee('Inicia sesión');
    }
}
