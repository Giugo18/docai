import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DettaglioDocumento } from './dettaglio-documento';

describe('DettaglioDocumento', () => {
  let component: DettaglioDocumento;
  let fixture: ComponentFixture<DettaglioDocumento>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DettaglioDocumento],
    }).compileComponents();

    fixture = TestBed.createComponent(DettaglioDocumento);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
